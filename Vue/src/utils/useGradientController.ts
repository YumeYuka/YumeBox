import { ref, computed } from "vue";
import type { RGBColor } from "./ColorUtils";
import { BgEffectDataManager, DeviceType, ThemeMode, type BgEffectData } from "./BgEffectDataManager";
import { floatArrayToRGBColor, rgbToHsv, hsvToRgb } from "./ColorUtils";

export interface GradientControllerOptions {
	deviceType?: DeviceType;
	themeMode?: ThemeMode;
	actionBarPadding?: number;
	logoPadding?: number;
	logoHeight?: number;
}

export function useGradientController(options: GradientControllerOptions = {}) {
	const dataManager = new BgEffectDataManager();

	const actionBarPadding = ref(options.actionBarPadding ?? 200);
	const logoPadding = ref(options.logoPadding ?? 300);
	const logoHeight = ref(options.logoHeight ?? 120);

	const scrollY = ref(0);

	let effectData: BgEffectData;
	let colorSets: number[][] = [];
	const time = ref(0);
	const currentColors = ref<RGBColor[]>([]);

	const scrollFactor = computed(() => {
		return Math.min(1.0, Math.max(0.0, Math.abs(scrollY.value) / actionBarPadding.value));
	});

	const logoScrollFactor = computed(() => {
		if (scrollY.value >= logoPadding.value) {
			return Math.min(1.0, Math.max(0.0, Math.abs(scrollY.value - logoPadding.value) / logoHeight.value));
		}
		return 0;
	});

	const initData = (deviceType: DeviceType, themeMode: ThemeMode) => {
		effectData = dataManager.getData(deviceType, themeMode);
		colorSets = [effectData.gradientColors1, effectData.gradientColors2, effectData.gradientColors3];

		const initialColors: RGBColor[] = [];
		for (let i = 0; i < 4; i++) {
			initialColors.push(floatArrayToRGBColor(effectData.gradientColors2, i));
		}
		currentColors.value = initialColors;
		time.value = 0;
	};

	const calculateColorWeights = (t: number): number[] => {
		const numSets = colorSets.length;
		const rawWeights: number[] = [];

		for (let i = 0; i < numSets; i++) {
			const phase = (i * 2 * Math.PI) / numSets;
			const cycleSpeed = (2 * Math.PI) / 8;

			const raw = Math.cos(t * cycleSpeed + phase);
			const normalized = (raw + 1) / 2;

			const minWeight = 0.25;
			const withFloor = minWeight + (1 - minWeight) * normalized;

			rawWeights.push(withFloor);
		}

		const sum = rawWeights.reduce((a, b) => a + b, 0);
		return sum > 0 ? rawWeights.map((w) => w / sum) : rawWeights.map(() => 1 / numSets);
	};

	const mixColorSets = (weights: number[]): RGBColor[] => {
		const numColors = 4;
		const result: RGBColor[] = [];

		for (let colorIndex = 0; colorIndex < numColors; colorIndex++) {
			let r = 0,
				g = 0,
				b = 0,
				a = 0;

			for (let setIndex = 0; setIndex < colorSets.length; setIndex++) {
				const weight = weights[setIndex] ?? 0;
				const offset = colorIndex * 4;
				const colors = colorSets[setIndex] ?? [];

				r += (colors[offset] ?? 0) * weight;
				g += (colors[offset + 1] ?? 0) * weight;
				b += (colors[offset + 2] ?? 0) * weight;
				a += (colors[offset + 3] ?? 1) * weight;
			}

			const hsv = rgbToHsv(r, g, b);
			hsv.s = Math.min(1.0, hsv.s * 1.15);
			const enhanced = hsvToRgb(hsv.h, hsv.s, hsv.v);
			result.push({ r: enhanced.r, g: enhanced.g, b: enhanced.b, a });
		}

		return result;
	};

	const updateFrame = (deltaTime: number) => {
		time.value += deltaTime * 1.2;

		const weights = calculateColorWeights(time.value);
		currentColors.value = mixColorSets(weights);
	};

	const getBackgroundAlpha = () => {
		return 1.0 - scrollFactor.value;
	};

	const getLogoAlpha = () => {
		if (scrollY.value >= logoPadding.value) {
			return 1.0 - logoScrollFactor.value;
		}
		return 1.0;
	};

	const getLogoScale = () => {
		if (scrollY.value >= logoPadding.value) {
			return 1.0 - 0.1 * logoScrollFactor.value;
		}
		return 1.0 - 0.1 * scrollFactor.value;
	};

	const getVersionAlpha = () => {
		if (logoPadding.value > 0) {
			return 1.0 - scrollFactor.value * (actionBarPadding.value / logoPadding.value);
		}
		return 1.0 - scrollFactor.value;
	};

	const setTheme = (deviceType: DeviceType, themeMode: ThemeMode) => {
		initData(deviceType, themeMode);
	};

	const updateScrollY = (newScrollY: number) => {
		scrollY.value = newScrollY;
	};

	const initialDeviceType = options.deviceType ?? (window.innerWidth >= 768 ? DeviceType.TABLET : DeviceType.PHONE);
	const initialThemeMode =
		options.themeMode ??
		(window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches ? ThemeMode.DARK : ThemeMode.LIGHT);

	initData(initialDeviceType, initialThemeMode);

	return {
		currentColors,
		scrollY,

		backgroundAlpha: computed(() => getBackgroundAlpha()),
		logoAlpha: computed(() => getLogoAlpha()),
		logoScale: computed(() => getLogoScale()),
		versionAlpha: computed(() => getVersionAlpha()),

		updateFrame,
		setTheme,
		updateScrollY,
	};
}
