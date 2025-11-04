export interface BgEffectData {
	uTranslateY: number;
	uPoints: number[];
	uAlphaMulti: number;
	uNoiseScale: number;
	uPointOffset: number;
	uPointRadiusMulti: number;
	uSaturateOffset: number;
	uLightOffset: number;
	uAlphaOffset: number;
	uShadowColorMulti: number;
	uShadowColorOffset: number;
	uShadowNoiseScale: number;
	uShadowOffset: number;
	colorInterpPeriod: number;
	gradientSpeedChange: number;
	gradientSpeedRest: number;
	gradientColors1: number[];
	gradientColors2: number[];
	gradientColors3: number[];
}

export type DeviceType = "PHONE" | "TABLET";
export const DeviceType = {
	PHONE: "PHONE" as DeviceType,
	TABLET: "TABLET" as DeviceType,
};

export type ThemeMode = "LIGHT" | "DARK";
export const ThemeMode = {
	LIGHT: "LIGHT" as ThemeMode,
	DARK: "DARK" as ThemeMode,
};

export class BgEffectDataManager {
	private dataPhoneLight: BgEffectData;
	private dataPadLight: BgEffectData;
	private dataPhoneDark: BgEffectData;
	private dataPadDark: BgEffectData;

	constructor() {
		this.dataPhoneLight = {
			uTranslateY: 0.0,
			uPoints: [0.8, 0.2, 1.0, 0.8, 0.9, 1.0, 0.2, 0.9, 1.0, 0.2, 0.2, 1.0],
			uAlphaMulti: 1.0,
			uNoiseScale: 1.5,
			uPointOffset: 0.2,
			uPointRadiusMulti: 1.0,
			uSaturateOffset: 0.2,
			uLightOffset: 0.1,
			uAlphaOffset: 0.5,
			uShadowColorMulti: 0.3,
			uShadowColorOffset: 0.3,
			uShadowNoiseScale: 5.0,
			uShadowOffset: 0.01,
			colorInterpPeriod: 5.0,
			gradientSpeedChange: 1.6,
			gradientSpeedRest: 1.05,
			gradientColors1: [1.0, 0.9, 0.94, 1.0, 1.0, 0.84, 0.89, 1.0, 0.97, 0.73, 0.82, 1.0, 0.64, 0.65, 0.98, 1.0],
			gradientColors2: [0.58, 0.74, 1.0, 1.0, 1.0, 0.9, 0.93, 1.0, 0.74, 0.76, 1.0, 1.0, 0.97, 0.77, 0.84, 1.0],
			gradientColors3: [0.98, 0.86, 0.9, 1.0, 0.6, 0.73, 0.98, 1.0, 0.92, 0.93, 1.0, 1.0, 0.56, 0.69, 1.0, 1.0],
		};

		this.dataPadLight = {
			uTranslateY: 0.0,
			uPoints: [0.8, 0.2, 1.0, 0.8, 0.9, 1.0, 0.2, 0.9, 1.0, 0.2, 0.2, 1.0],
			uAlphaMulti: 1.0,
			uNoiseScale: 1.5,
			uPointOffset: 0.2,
			uPointRadiusMulti: 1.0,
			uSaturateOffset: 0.2,
			uLightOffset: 0.1,
			uAlphaOffset: 0.5,
			uShadowColorMulti: 0.3,
			uShadowColorOffset: 0.3,
			uShadowNoiseScale: 5.0,
			uShadowOffset: 0.01,
			colorInterpPeriod: 7.0,
			gradientSpeedChange: 1.8,
			gradientSpeedRest: 1.0,
			gradientColors1: [0.99, 0.77, 0.86, 1.0, 0.74, 0.76, 1.0, 1.0, 0.72, 0.74, 1.0, 1.0, 0.98, 0.76, 0.8, 1.0],
			gradientColors2: [0.66, 0.75, 1.0, 1.0, 1.0, 0.86, 0.91, 1.0, 0.74, 0.76, 1.0, 1.0, 0.97, 0.77, 0.84, 1.0],
			gradientColors3: [0.97, 0.79, 0.85, 1.0, 0.65, 0.68, 0.98, 1.0, 0.66, 0.77, 1.0, 1.0, 0.72, 0.73, 0.98, 1.0],
		};

		this.dataPhoneDark = {
			uTranslateY: 0.0,
			uPoints: [0.8, 0.2, 1.0, 0.8, 0.9, 1.0, 0.2, 0.9, 1.0, 0.2, 0.2, 1.0],
			uAlphaMulti: 1.0,
			uNoiseScale: 1.5,
			uPointOffset: 0.4,
			uPointRadiusMulti: 1.0,
			uSaturateOffset: 0.17,
			uLightOffset: 0.0,
			uAlphaOffset: 0.5,
			uShadowColorMulti: 0.3,
			uShadowColorOffset: 0.3,
			uShadowNoiseScale: 5.0,
			uShadowOffset: 0.01,
			colorInterpPeriod: 8.0,
			gradientSpeedChange: 1.0,
			gradientSpeedRest: 1.0,
			gradientColors1: [0.2, 0.06, 0.88, 0.4, 0.3, 0.14, 0.55, 0.5, 0.0, 0.64, 0.96, 0.5, 0.11, 0.16, 0.83, 0.4],
			gradientColors2: [0.07, 0.15, 0.79, 0.5, 0.62, 0.21, 0.67, 0.5, 0.06, 0.25, 0.84, 0.5, 0.0, 0.2, 0.78, 0.5],
			gradientColors3: [0.58, 0.3, 0.74, 0.4, 0.27, 0.18, 0.6, 0.5, 0.66, 0.26, 0.62, 0.5, 0.12, 0.16, 0.7, 0.6],
		};

		this.dataPadDark = {
			uTranslateY: 0.0,
			uPoints: [0.8, 0.2, 1.0, 0.8, 0.9, 1.0, 0.2, 0.9, 1.0, 0.2, 0.2, 1.0],
			uAlphaMulti: 1.0,
			uNoiseScale: 1.5,
			uPointOffset: 0.2,
			uPointRadiusMulti: 1.0,
			uSaturateOffset: 0.0,
			uLightOffset: 0.0,
			uAlphaOffset: 0.5,
			uShadowColorMulti: 0.3,
			uShadowColorOffset: 0.3,
			uShadowNoiseScale: 5.0,
			uShadowOffset: 0.01,
			colorInterpPeriod: 7.0,
			gradientSpeedChange: 1.6,
			gradientSpeedRest: 1.2,
			gradientColors1: [0.66, 0.26, 0.62, 0.4, 0.06, 0.25, 0.84, 0.5, 0.0, 0.64, 0.96, 0.5, 0.14, 0.18, 0.55, 0.5],
			gradientColors2: [0.07, 0.15, 0.79, 0.5, 0.11, 0.16, 0.83, 0.5, 0.06, 0.25, 0.84, 0.5, 0.66, 0.26, 0.62, 0.5],
			gradientColors3: [0.58, 0.3, 0.74, 0.5, 0.11, 0.16, 0.83, 0.5, 0.66, 0.26, 0.62, 0.5, 0.27, 0.18, 0.6, 0.6],
		};
	}

	getData(deviceType: DeviceType, themeMode: ThemeMode): BgEffectData {
		if (deviceType === DeviceType.PHONE) {
			return themeMode === ThemeMode.LIGHT ? this.dataPhoneLight : this.dataPhoneDark;
		} else {
			return themeMode === ThemeMode.LIGHT ? this.dataPadLight : this.dataPadDark;
		}
	}
}
