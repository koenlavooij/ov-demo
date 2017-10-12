function CanvasContextMap(canvasContext2D, overlayView) {
    const project = function(pts) {
        return pts
            .map(function(latlong) { return overlayView.getProjection().fromLatLngToDivPixel(latlong) })
            .reduce(function(result, pt) {
                result.push(pt.x, pt.y); return result }, []);
    };
    this.fillStyle = canvasContext2D.fillStyle;
    this.strokeStyle = canvasContext2D.strokeStyle;
    this.shadowColor = canvasContext2D.shadowColor;
    this.shadowBlur = canvasContext2D.shadowBlur;
    this.shadowOffsetX = canvasContext2D.shadowOffsetX;
    this.shadowOffsetY = canvasContext2D.shadowOffsetY;
    this.lineCap = canvasContext2D.lineCap;
    this.lineJoin = canvasContext2D.lineJoin;
    this.lineWidth = canvasContext2D.lineWidth;
    this.miterLimit = canvasContext2D.miterLimit;
    this.font = canvasContext2D.font;
    this.textAlign = canvasContext2D.textAlign;
    this.textBaseline = canvasContext2D.textBaseline;
    this.globalAlpha = canvasContext2D.globalAlpha;
    this.globalCompositeOperation = canvasContext2D.globalCompositeOperation;

    this.scale = function() {}; // no-op
    this.rotate = function() {}; // no-op
    this.translate = function() {}; // no-op
    this.transform = function() {}; // no-op
    this.setTransform = function() {}; // no-op

    this.createLinearGradient = function() { return canvasContext2D.createLinearGradient() };
    this.createPattern = function() { return canvasContext2D.createPattern() };
    this.createRadialGradient = function() { return canvasContext2D.createRadialGradient() };
    this.addColorStop = function() { return canvasContext2D.addColorStop };
    this.fill = function() { canvasContext2D.fill() };
    this.stroke = function() { canvasContext2D.stroke() };
    this.clip = function() { canvasContext2D.clip() };
    this.measureText = function(text) { return canvasContext2D.measureText(text) };
    this.createImageData = function() { return canvasContext2D.createImageData() };
    this.getImageData = function() { return canvasContext2D.getImageData() };
    this.putImageData = function() { return canvasContext2D.putImageData() };
    this.beginPath = function() { canvasContext2D.beginPath() };
    this.closePath = function() { canvasContext2D.closePath() };
    this.rect = function(ll1, ll2) {
        canvasContext2D.rect.apply(canvasContext2D, project([ll1, ll2]))
    };
    this.fillRect = function(ll1, ll2) {
        canvasContext2D.fillRect.apply(canvasContext2D, project([ll1, ll2]))
    };
    this.strokeRect = function(ll1, ll2) {
        canvasContext2D.strokeRect.apply(canvasContext2D, project([ll1, ll2]))
    };
    this.clearRect = function(ll1, ll2) {
        canvasContext2D.clearRect.apply(canvasContext2D, project([ll1, ll2]))
    };
    this.moveTo = function(ll) {
        canvasContext2D.moveTo.apply(canvasContext2D, project(ll));
    };
    this.lineTo = function(ll) {
        canvasContext2D.lineTo.apply(canvasContext2D, project(ll));
    };
    this.quadraticCurveTo = function(ll1, ll2) {
        canvasContext2D.quadraticCurveTo.apply(canvasContext2D, project([ll1, ll2]))
    };
    this.bezierCurveTo = function(ll1, ll2) {
        canvasContext2D.bezierCurveTo.apply(canvasContext2D, project([ll1, ll2]))
    };
    this.arc = function(center, edge, start, end, reverse) {
        const pts = project([center, edge]);

        const dist = Math.sqrt(
            Math.max(1,
                ((pts[0] - pts[2]) * (pts[0] - pts[2])) +
                ((pts[1] - pts[3]) * (pts[1] - pts[3]))));

        canvasContext2D.arc.apply(canvasContext2D, [pts[0], pts[1], dist, start, end, reverse]);
    };
    this.arcTo = function(ll1, ll2) {}; //not implemented yet
    this.isPointInPath = function(ll) {
        canvasContext2D.isPointInPath.apply(canvasContext2D, project(ll));
    };
    this.fillText = function(text, ll) {
        const p = project(ll);
        canvasContext2D.fillText(canvasContext2D, [text, p[0], p[1]]);
    };
    this.strokeText = function(text, ll) {
        const p = project(ll);
        canvasContext2D.strokeText(canvasContext2D, [text, p[0], p[1]]);
    };
    this.drawImage = function(img, ll) {
        const p = project(ll);
        canvasContext2D.drawImage(canvasContext2D, [img, p[0], p[1]]);
    }
}