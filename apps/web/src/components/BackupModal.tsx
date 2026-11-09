import React, { useState } from 'react';
import { DownloadCloud, UploadCloud, CheckCircle2, AlertCircle, FileArchive } from 'lucide-react';
import { ApiClient } from '../api/client.js';

interface BackupModalProps {
  isOpen: boolean;
  onClose: () => void;
  onRestored: () => Promise<void>;
}

export const BackupModal: React.FC<BackupModalProps> = ({
  isOpen,
  onClose,
  onRestored
}) => {
  const [isExporting, setIsExporting] = useState(false);
  const [isImporting, setIsImporting] = useState(false);
  const [importStatus, setImportStatus] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleExport = async () => {
    setIsExporting(true);
    setErrorMessage(null);
    try {
      await ApiClient.downloadBackup();
    } catch (err: any) {
      setErrorMessage(err.message || 'Export failed');
    } finally {
      setIsExporting(false);
    }
  };

  const handleFileSelect = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setIsImporting(true);
    setErrorMessage(null);
    setImportStatus(null);
    try {
      const res = await ApiClient.uploadBackup(file);
      setImportStatus(`Successfully restored ${res.restoredItems} items and ${res.restoredOutfits} outfits.`);
      await onRestored();
    } catch (err: any) {
      setErrorMessage(err.message || 'Import failed: make sure you selected a valid Trousseau ZIP archive.');
    } finally {
      setIsImporting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-stone-900/50 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white rounded-3xl max-w-md w-full p-6 space-y-6 shadow-2xl">
        <div className="flex justify-between items-center border-b border-stone-100 pb-3">
          <div className="flex items-center space-x-2">
            <FileArchive className="w-5 h-5 text-amber-700" />
            <h3 className="text-xl font-bold font-serif text-stone-900">Backup & Data Portability</h3>
          </div>
          <button onClick={onClose} className="text-stone-400 hover:text-stone-600 font-bold">
            ✕
          </button>
        </div>

        <p className="text-stone-600 text-xs leading-relaxed">
          As a self-hosted user, your data belongs exclusively to you. Export or restore your entire wardrobe, outfits, daily logs, ratings, and original photos in a single portable <code className="text-amber-800 font-mono">.zip</code> archive.
        </p>

        {errorMessage && (
          <div className="p-3 bg-red-50 text-red-700 border border-red-200 rounded-xl text-xs flex items-center space-x-2">
            <AlertCircle className="w-4 h-4 flex-shrink-0" />
            <span>{errorMessage}</span>
          </div>
        )}

        {importStatus && (
          <div className="p-3 bg-emerald-50 text-emerald-800 border border-emerald-200 rounded-xl text-xs flex items-center space-x-2">
            <CheckCircle2 className="w-4 h-4 flex-shrink-0" />
            <span>{importStatus}</span>
          </div>
        )}

        <div className="space-y-4">
          {/* Export Box */}
          <div className="bg-stone-50 rounded-2xl p-4 border border-stone-200 space-y-2">
            <h4 className="text-xs font-bold text-stone-900 uppercase tracking-wider">Export Backup</h4>
            <p className="text-[11px] text-stone-500">
              Downloads a complete snapshot of all clothes, images, wear history, and database records.
            </p>
            <button
              onClick={handleExport}
              disabled={isExporting}
              className="w-full mt-2 py-2.5 bg-stone-900 hover:bg-stone-800 text-white rounded-xl text-xs font-semibold flex items-center justify-center space-x-2 shadow-sm transition-colors disabled:opacity-50"
            >
              <DownloadCloud className="w-4 h-4" />
              <span>{isExporting ? 'Generating ZIP...' : 'Download ZIP Backup'}</span>
            </button>
          </div>

          {/* Import Box */}
          <div className="bg-stone-50 rounded-2xl p-4 border border-stone-200 space-y-2">
            <h4 className="text-xs font-bold text-stone-900 uppercase tracking-wider">Restore / Import Backup</h4>
            <p className="text-[11px] text-stone-500">
              Upload a previously exported Trousseau ZIP archive to restore your data and photos.
            </p>
            <label className="w-full mt-2 py-2.5 bg-white border border-stone-300 hover:border-amber-600 text-stone-800 rounded-xl text-xs font-semibold flex items-center justify-center space-x-2 cursor-pointer shadow-sm transition-colors">
              <UploadCloud className="w-4 h-4 text-amber-700" />
              <span>{isImporting ? 'Restoring data...' : 'Upload & Restore ZIP'}</span>
              <input
                type="file"
                accept=".zip"
                disabled={isImporting}
                onChange={handleFileSelect}
                className="hidden"
              />
            </label>
          </div>
        </div>
      </div>
    </div>
  );
};
