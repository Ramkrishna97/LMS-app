import React, { useState } from 'react';
import Cropper from 'react-easy-crop';

async function croppedFile(source, pixels, name, maxDimension) {
  const image = new Image();
  image.src = source;
  await new Promise((resolve, reject) => {
    image.onload = resolve;
    image.onerror = () => reject(new Error('This image format cannot be decoded by your browser. Convert it to PNG or JPEG and try again.'));
  });
  const scale = Math.min(1, maxDimension / Math.max(pixels.width, pixels.height));
  const canvas = document.createElement('canvas');
  canvas.width = Math.max(1, Math.round(pixels.width * scale));
  canvas.height = Math.max(1, Math.round(pixels.height * scale));
  const context = canvas.getContext('2d');
  if (!context) throw new Error('Your browser could not prepare the image canvas.');
  context.fillStyle = '#fff';
  context.fillRect(0, 0, canvas.width, canvas.height);
  context.drawImage(image, pixels.x, pixels.y, pixels.width, pixels.height, 0, 0, canvas.width, canvas.height);
  const blob = await new Promise(resolve => canvas.toBlob(resolve, 'image/jpeg', 0.86));
  if (!blob) throw new Error('Could not convert this image to JPEG.');
  const base = (name || 'image').replace(/\.[^.]+$/, '');
  return new File([blob], `${base}-cropped.jpg`, { type: 'image/jpeg' });
}

export default function ImageCropper({ source, name, onDone, onCancel, aspect = 16 / 9, maxDimension = 1920 }) {
  const [crop, setCrop] = useState({ x: 0, y: 0 });
  const [zoom, setZoom] = useState(1);
  const [pixels, setPixels] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const apply = async () => {
    setBusy(true);
    setError('');
    try { await onDone(await croppedFile(source, pixels, name, maxDimension)); }
    catch (err) { setError(err.message); }
    finally { setBusy(false); }
  };
  return <div className="modal-backdrop-custom">
    <div className="dialog crop-dialog" role="dialog" aria-modal="true" aria-label="Crop image">
      <h3>Crop image</h3>
      <p className="text-secondary">Drag to position the image, then use the slider to zoom.</p>
      {error && <div className="alert alert-danger">{error}</div>}
      <div className="crop-area"><Cropper image={source} crop={crop} zoom={zoom} aspect={aspect}
        onCropChange={setCrop} onZoomChange={setZoom} onCropComplete={(_, area) => setPixels(area)} /></div>
      <label className="form-label mt-3" htmlFor="crop-zoom">Zoom</label>
      <input id="crop-zoom" className="form-range" type="range" min="1" max="3" step="0.05"
        value={zoom} onChange={e => setZoom(Number(e.target.value))} />
      <div className="d-flex justify-content-end gap-2">
        <button type="button" className="btn btn-outline-secondary" onClick={onCancel}>Cancel</button>
        <button type="button" className="btn btn-primary" disabled={busy || !pixels} onClick={apply}>
          {busy ? 'Processing…' : 'Use cropped image'}
        </button>
      </div>
    </div>
  </div>;
}
