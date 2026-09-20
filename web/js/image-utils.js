// ==========================================
// IMAGE PROCESSING & CLOUDINARY UTILITIES
// ==========================================

export function processSpiritImage(fileOrBlob, maxWidth = 512) {
  return new Promise((resolve, reject) => {
    if (!fileOrBlob) return reject(new Error("No se proporcionó ningún archivo de imagen"));
    const reader = new FileReader();
    reader.onload = function(e) {
      const img = new Image();
      img.onload = function() {
        try {
          const tempCanvas = document.createElement('canvas');
          const origW = img.naturalWidth || img.width;
          const origH = img.naturalHeight || img.height;
          tempCanvas.width = origW;
          tempCanvas.height = origH;
          const tempCtx = tempCanvas.getContext('2d', { willReadFrequently: true });
          tempCtx.drawImage(img, 0, 0);

          const imgData = tempCtx.getImageData(0, 0, origW, origH);
          const data = imgData.data;

          let minX = origW, minY = origH, maxX = -1, maxY = -1;
          let hasTransparency = false;

          for (let y = 0; y < origH; y++) {
            for (let x = 0; x < origW; x++) {
              const idx = (y * origW + x) * 4;
              const alpha = data[idx + 3];
              if (alpha < 240) {
                hasTransparency = true;
              }
              if (alpha > 15) {
                if (x < minX) minX = x;
                if (x > maxX) maxX = x;
                if (y < minY) minY = y;
                if (y > maxY) maxY = y;
              }
            }
          }

          if (maxX < minX || maxY < minY) {
            minX = 0; minY = 0; maxX = origW - 1; maxY = origH - 1;
          }

          const contentW = maxX - minX + 1;
          const contentH = maxY - minY + 1;

          const finalCanvas = document.createElement('canvas');
          let finalCtx = finalCanvas.getContext('2d');

          if (hasTransparency) {
            const pad = Math.max(2, Math.round(Math.max(contentW, contentH) * 0.04));
            const targetDim = Math.min(maxWidth, Math.max(contentW, contentH) + pad * 2);

            finalCanvas.width = targetDim;
            finalCanvas.height = targetDim;
            finalCtx = finalCanvas.getContext('2d');
            finalCtx.imageSmoothingEnabled = false;

            const scale = (targetDim - pad * 2) / Math.max(contentW, contentH);
            const drawW = Math.round(contentW * scale);
            const drawH = Math.round(contentH * scale);
            const drawX = Math.round((targetDim - drawW) / 2);
            const drawY = Math.round((targetDim - drawH) / 2);

            finalCtx.drawImage(
              img,
              minX, minY, contentW, contentH,
              drawX, drawY, drawW, drawH
            );

            resolve({
              dataUrl: finalCanvas.toDataURL('image/png'),
              hasTransparency: true,
              trimmed: (contentW < origW * 0.9 || contentH < origH * 0.9)
            });
          } else {
            let outW = origW;
            let outH = origH;
            if (outW > maxWidth || outH > maxWidth) {
              if (outW > outH) {
                outH = Math.round((outH * maxWidth) / outW);
                outW = maxWidth;
              } else {
                outW = Math.round((outW * maxWidth) / outH);
                outH = maxWidth;
              }
            }
            finalCanvas.width = outW;
            finalCanvas.height = outH;
            finalCtx = finalCanvas.getContext('2d');
            finalCtx.drawImage(img, 0, 0, outW, outH);

            resolve({
              dataUrl: finalCanvas.toDataURL('image/png'),
              hasTransparency: false,
              trimmed: false
            });
          }
        } catch (procErr) {
          console.error("Error al procesar píxeles:", procErr);
          resolve({
            dataUrl: e.target.result,
            hasTransparency: false,
            trimmed: false
          });
        }
      };
      img.onerror = () => reject(new Error("No se pudo procesar la imagen seleccionada"));
      img.src = e.target.result;
    };
    reader.onerror = () => reject(new Error("Error leyendo el archivo"));
    reader.readAsDataURL(fileOrBlob);
  });
}

export function compressImage(file, maxWidth = 512) {
  return processSpiritImage(file, maxWidth).then(res => res.dataUrl);
}

export function optimizeCloudinaryUrl(url, width = 180) {
  if (!url || typeof url !== 'string') return url;
  if (url.includes('res.cloudinary.com') && url.includes('/upload/')) {
    if (!url.includes('/f_auto,q_auto')) {
      return url.replace('/upload/', `/upload/f_auto,q_auto,w_${width}/`);
    }
  }
  return url;
}

export function getOptimizedCloudinaryUrl(url, width = 180) {
  return optimizeCloudinaryUrl(url, width);
}

export function trimCanvasTransparency(canvas) {
  const ctx = canvas.getContext('2d', { willReadFrequently: true });
  const w = canvas.width;
  const h = canvas.height;
  const imgData = ctx.getImageData(0, 0, w, h);
  const data = imgData.data;

  let minX = w, minY = h, maxX = -1, maxY = -1;

  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      const alpha = data[(y * w + x) * 4 + 3];
      if (alpha > 10) {
        if (x < minX) minX = x;
        if (x > maxX) maxX = x;
        if (y < minY) minY = y;
        if (y > maxY) maxY = y;
      }
    }
  }

  if (maxX < minX || maxY < minY) {
    return canvas;
  }

  const trimW = maxX - minX + 1;
  const trimH = maxY - minY + 1;

  const trimmedCanvas = document.createElement('canvas');
  trimmedCanvas.width = trimW;
  trimmedCanvas.height = trimH;
  const trimmedCtx = trimmedCanvas.getContext('2d');
  trimmedCtx.drawImage(canvas, minX, minY, trimW, trimH, 0, 0, trimW, trimH);

  return trimmedCanvas;
}

