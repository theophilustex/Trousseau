package com.trousseau.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tag", uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "name"}))
@NamedQueries({
    @NamedQuery(name = "Tag.findByOwner", query = "SELECT t FROM Tag t WHERE t.owner = :owner ORDER BY t.name"),
    @NamedQuery(name = "Tag.findByOwnerAndName", query = "SELECT t FROM Tag t WHERE t.owner = :owner AND t.name = :name")
})
@Getter @Setter
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "name"})
public class Tag implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @NotNull
    @Size(min = 1, max = 50)
    @Column(nullable = false, length = 50)
    private String name;

    @ManyToMany(mappedBy = "tags")
    private Set<ClothingItem> clothingItems = new HashSet<>();
}
