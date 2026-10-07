package com.trousseau.scheduler;

import com.trousseau.util.AppConfig;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.Timeout;
import jakarta.ejb.TimerConfig;
import jakarta.ejb.TimerService;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Deletes PostgreSQL large objects that no clothing item refers to any more.
 *
 * <p>On PostgreSQL, Hibernate stores each photo, thumbnail and receipt as a large object
 * and keeps only its id in the row. Removing or replacing the row's reference never
 * removes the object, so deleting a receipt or an item leaves its bytes behind. Before
 * ClothingItem's BLOBs became truly lazy, every save of an item (a recorded wear, a wash)
 * also rewrote all three as new objects and orphaned the old ones, so older databases
 * can hold far more orphaned data than live data.</p>
 *
 * <p>Runs a minute after start-up and nightly. It deletes only objects that are owned by
 * the app's own database user and referenced by no item, in small batches, each in its own
 * transaction. An object created by an upload that has not committed yet is invisible to
 * the sweep, so it cannot be removed mid-upload. Does nothing on H2, where BLOBs are stored
 * in the row, or when TROUSSEAU_LARGE_OBJECT_SWEEP=false.</p>
 */
@Singleton
@Startup
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class LargeObjectSweeper {

    private static final Logger LOG = Logger.getLogger(LargeObjectSweeper.class.getName());

    private static final long STARTUP_DELAY_MS = 60_000;
    private static final int BATCH_SIZE = 200;

    /** Orphans: owned by the current database user, and not referenced by any item column. */
    private static final String UNLINK_BATCH =
            "SELECT lo_unlink(m.oid) FROM ("
          + "  SELECT oid FROM pg_largeobject_metadata"
          + "  WHERE lomowner = (SELECT oid FROM pg_roles WHERE rolname = current_user)"
          + "    AND NOT EXISTS (SELECT 1 FROM clothing_item c WHERE pg_largeobject_metadata.oid"
          + "                    IN (c.image_data, c.thumbnail_data, c.receipt_data))"
          + "  LIMIT " + BATCH_SIZE + ") m";

    /** The sweep is only meaningful when all three BLOB columns hold large-object ids. */
    private static final String OID_COLUMNS =
            "SELECT count(*) FROM information_schema.columns WHERE table_name = 'clothing_item'"
          + " AND column_name IN ('image_data', 'thumbnail_data', 'receipt_data') AND data_type = 'oid'";

    /**
     * Any other large-object column would make the sweep wrong: its objects would look
     * unreferenced and be deleted. If one is ever added, extend UNLINK_BATCH first; until
     * then this check makes the sweep refuse to run.
     */
    private static final String OTHER_OID_COLUMNS =
            "SELECT string_agg(table_name || '.' || column_name, ', ') FROM information_schema.columns"
          + " WHERE data_type = 'oid' AND table_schema NOT IN ('pg_catalog', 'information_schema')"
          + " AND NOT (table_name = 'clothing_item' AND column_name IN ('image_data', 'thumbnail_data', 'receipt_data'))";

    @Resource(lookup = "java:comp/DefaultDataSource")
    private DataSource dataSource;

    @Resource
    private TimerService timerService;

    @PostConstruct
    void scheduleStartupSweep() {
        // Not in @PostConstruct itself: a first sweep of a large backlog must not hold up
        // deployment.
        timerService.createSingleActionTimer(STARTUP_DELAY_MS, new TimerConfig("startup sweep", false));
    }

    @Timeout
    public void startupSweep() {
        sweep();
    }

    @Schedule(hour = "3", minute = "30", persistent = false)
    public void nightlySweep() {
        sweep();
    }

    /** Runs a sweep now. Returns how many large objects were deleted (0 when skipped). */
    public int sweep() {
        if (!AppConfig.largeObjectSweepEnabled()) {
            LOG.fine("Large-object sweep disabled by TROUSSEAU_LARGE_OBJECT_SWEEP");
            return 0;
        }
        try (Connection conn = dataSource.getConnection()) {
            if (!applies(conn)) {
                return 0;
            }
            conn.setAutoCommit(true);   // one transaction per batch
            int total = 0;
            int batch;
            try (Statement st = conn.createStatement()) {
                do {
                    batch = 0;
                    try (ResultSet rs = st.executeQuery(UNLINK_BATCH)) {
                        while (rs.next()) {
                            batch++;
                        }
                    }
                    total += batch;
                } while (batch == BATCH_SIZE);
            }
            if (total > 0) {
                LOG.info("Large-object sweep removed " + total + " orphaned photo/receipt object(s)");
            } else {
                LOG.fine("Large-object sweep found nothing to remove");
            }
            return total;
        } catch (SQLException | RuntimeException e) {
            LOG.log(Level.WARNING, "Large-object sweep failed; it will run again tonight", e);
            return 0;
        }
    }

    private boolean applies(Connection conn) throws SQLException {
        String product = conn.getMetaData().getDatabaseProductName();
        if (product == null || !product.toLowerCase().contains("postgresql")) {
            LOG.fine("Large-object sweep skipped: database is " + product);
            return false;
        }
        try (PreparedStatement ps = conn.prepareStatement(OID_COLUMNS);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next() || rs.getInt(1) != 3) {
                LOG.warning("Large-object sweep skipped: clothing_item's BLOB columns are not all of type oid");
                return false;
            }
        }
        try (PreparedStatement ps = conn.prepareStatement(OTHER_OID_COLUMNS);
             ResultSet rs = ps.executeQuery()) {
            String others = rs.next() ? rs.getString(1) : null;
            if (others != null) {
                LOG.warning("Large-object sweep skipped: other large-object columns exist (" + others
                        + ") and the sweep does not know about them");
                return false;
            }
        }
        return true;
    }
}
