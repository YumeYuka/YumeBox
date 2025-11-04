export interface RGBColor {
	r: number;
	g: number;
	b: number;
	a?: number;
}

export function rgbToHsv(r: number, g: number, b: number): { h: number; s: number; v: number } {
	const max = Math.max(r, g, b);
	const min = Math.min(r, g, b);
	const delta = max - min;

	let h = 0;
	if (delta !== 0) {
		if (max === r) {
			h = 60 * (((g - b) / delta) % 6);
		} else if (max === g) {
			h = 60 * ((b - r) / delta + 2);
		} else {
			h = 60 * ((r - g) / delta + 4);
		}
	}

	if (h < 0) h += 360;

	const s = max === 0 ? 0 : delta / max;
	const v = max;

	return { h, s, v };
}

export function hsvToRgb(h: number, s: number, v: number): RGBColor {
	const c = v * s;
	const x = c * (1 - Math.abs(((h / 60) % 2) - 1));
	const m = v - c;

	let r1 = 0,
		g1 = 0,
		b1 = 0;

	if (h < 60) {
		r1 = c;
		g1 = x;
		b1 = 0;
	} else if (h < 120) {
		r1 = x;
		g1 = c;
		b1 = 0;
	} else if (h < 180) {
		r1 = 0;
		g1 = c;
		b1 = x;
	} else if (h < 240) {
		r1 = 0;
		g1 = x;
		b1 = c;
	} else if (h < 300) {
		r1 = x;
		g1 = 0;
		b1 = c;
	} else {
		r1 = c;
		g1 = 0;
		b1 = x;
	}

	return {
		r: r1 + m,
		g: g1 + m,
		b: b1 + m,
	};
}

export function deepen(color: RGBColor, saturationBoost: number = 0.4, darknessFactor: number = 0.7): RGBColor {
	const hsv = rgbToHsv(color.r, color.g, color.b);

	hsv.s = Math.min(1.0, hsv.s + saturationBoost);

	hsv.v = hsv.v * darknessFactor;

	const rgb = hsvToRgb(hsv.h, hsv.s, hsv.v);

	return {
		r: rgb.r,
		g: rgb.g,
		b: rgb.b,
		a: color.a,
	};
}

export function linearInterpolate(result: number[], start: number[], end: number[], t: number): void {
	for (let i = 0; i < start.length; i++) {
		const startVal = start[i];
		const endVal = end[i];
		if (startVal !== undefined && endVal !== undefined) {
			result[i] = startVal + (endVal - startVal) * t;
		}
	}
}

export function floatArrayToRGBColor(floatArray: number[], index: number): RGBColor {
	const offset = index * 4;
	return {
		r: floatArray[offset] ?? 0,
		g: floatArray[offset + 1] ?? 0,
		b: floatArray[offset + 2] ?? 0,
		a: floatArray[offset + 3] ?? 1,
	};
}

export function rgbToCSS(color: RGBColor): string {
	const r = Math.round(color.r * 255);
	const g = Math.round(color.g * 255);
	const b = Math.round(color.b * 255);
	const a = color.a ?? 1;
	return `rgba(${r}, ${g}, ${b}, ${a})`;
}

export function floatArrayToCSS(floatArray: number[], index: number): string {
	const offset = index * 4;
	const r = Math.round((floatArray[offset] ?? 0) * 255);
	const g = Math.round((floatArray[offset + 1] ?? 0) * 255);
	const b = Math.round((floatArray[offset + 2] ?? 0) * 255);
	const a = floatArray[offset + 3] ?? 1;
	return `rgba(${r}, ${g}, ${b}, ${a})`;
}
