<template>
	<div class="animated-gradient-background" :style="backgroundStyle"></div>
</template>

<script setup lang="ts">
	import { ref, computed, onMounted, onUnmounted } from "vue";
	import { BgEffectDataManager, DeviceType, ThemeMode, type BgEffectData } from "../utils/BgEffectDataManager";
	import { linearInterpolate, floatArrayToCSS, type RGBColor, floatArrayToRGBColor } from "../utils/ColorUtils";

	const props = defineProps<{
		alpha?: number;
	}>();

	const emit = defineEmits<{
		(e: "colorUpdate", colors: RGBColor[]): void;
	}>();

	const dataManager = new BgEffectDataManager();
	let currentData: BgEffectData;

	const uAnimTime = ref(0);
	const uColors = ref<number[]>(new Array(16).fill(0));
	const cycleCount = ref(0);
	const prevT = ref(0);
	const colorInterpT = ref(0);
	const gradientSpeed = ref(1.0);

	let startColorValue: number[] = [];
	let endColorValue: number[] = [];
	let animationFrameId: number | null = null;
	let lastTime = 0;

	const getDeviceType = (): DeviceType => {
		return window.innerWidth >= 768 ? DeviceType.TABLET : DeviceType.PHONE;
	};

	const getThemeMode = (): ThemeMode => {
		if (window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches) {
			return ThemeMode.DARK;
		}
		return ThemeMode.LIGHT;
	};

	const initData = () => {
		const deviceType = getDeviceType();
		const themeMode = getThemeMode();
		currentData = dataManager.getData(deviceType, themeMode);

		startColorValue = [...currentData.gradientColors2];
		endColorValue = [...currentData.gradientColors2];
		uColors.value = [...currentData.gradientColors2];

		uAnimTime.value = 0;
		cycleCount.value = 0;
		prevT.value = 0;
		colorInterpT.value = 0;
		gradientSpeed.value = currentData.gradientSpeedRest;
	};

	let lastEmitTime = 0;
	const emitThrottle = 100;

	const computeGradientColor = () => {
		const d = uAnimTime.value / currentData.colorInterpPeriod;
		const floor = (d - Math.floor(d)) * 2.0;

		if (Math.abs(prevT.value - floor) > 0.5) {
			const cycle = cycleCount.value % 4;

			if (cycle === 0) {
				startColorValue = [...currentData.gradientColors2];
				endColorValue = [...currentData.gradientColors1];
				executeAnim();
			} else if (cycle === 1) {
				startColorValue = [...currentData.gradientColors1];
				endColorValue = [...currentData.gradientColors2];
				executeAnim();
			} else if (cycle === 2) {
				startColorValue = [...currentData.gradientColors2];
				endColorValue = [...currentData.gradientColors3];
				executeAnim();
			} else if (cycle === 3) {
				startColorValue = [...currentData.gradientColors3];
				endColorValue = [...currentData.gradientColors2];
				executeAnim();
			}

			cycleCount.value += 1;
		}

		prevT.value = floor;

		linearInterpolate(uColors.value, startColorValue, endColorValue, colorInterpT.value);

		const now = performance.now();
		if (now - lastEmitTime > emitThrottle) {
			const rgbColors: RGBColor[] = [];
			for (let i = 0; i < 4; i++) {
				rgbColors.push(floatArrayToRGBColor(uColors.value, i));
			}
			emit("colorUpdate", rgbColors);
			lastEmitTime = now;
		}
	};

	const executeAnim = () => {
		colorInterpT.value = 0;

		const startTime = performance.now();
		const duration = 1200;

		const animate = (currentTime: number) => {
			const elapsed = currentTime - startTime;
			const progress = Math.min(elapsed / duration, 1);

			colorInterpT.value = progress;

			if (progress < 1) {
				requestAnimationFrame(animate);
			}
		};

		requestAnimationFrame(animate);
	};

	const backgroundStyle = computed(() => {
		const color1 = floatArrayToCSS(uColors.value, 0);
		const color2 = floatArrayToCSS(uColors.value, 1);
		const color3 = floatArrayToCSS(uColors.value, 2);
		const color4 = floatArrayToCSS(uColors.value, 3);

		const alpha = props.alpha ?? 1;

		return {
			background: `
				radial-gradient(circle at 80% 20%, ${color1} 0%, transparent 50%),
				radial-gradient(circle at 20% 80%, ${color2} 0%, transparent 50%),
				radial-gradient(circle at 80% 80%, ${color3} 0%, transparent 50%),
				radial-gradient(circle at 20% 20%, ${color4} 0%, transparent 50%),
				linear-gradient(135deg, ${color1}, ${color2}, ${color3}, ${color4})
			`,
			opacity: alpha,
			transition: "background 1.2s linear, opacity 0.3s linear",
		};
	});

	const updateFrame = (deltaTime: number) => {
		uAnimTime.value += deltaTime * gradientSpeed.value * 4.0;
		computeGradientColor();
	};

	const animationLoop = (currentTime: number) => {
		if (lastTime === 0) {
			lastTime = currentTime;
		}

		const deltaTime = (currentTime - lastTime) / 1000;
		lastTime = currentTime;

		updateFrame(deltaTime);
		animationFrameId = requestAnimationFrame(animationLoop);
	};

	const start = () => {
		if (animationFrameId === null) {
			lastTime = 0;
			animationFrameId = requestAnimationFrame(animationLoop);
		}
	};

	const stop = () => {
		if (animationFrameId !== null) {
			cancelAnimationFrame(animationFrameId);
			animationFrameId = null;
		}
	};

	onMounted(() => {
		initData();
		start();

		const darkModeQuery = window.matchMedia("(prefers-color-scheme: dark)");
		const handleThemeChange = () => {
			stop();
			initData();
			start();
		};

		darkModeQuery.addEventListener("change", handleThemeChange);

		const handleResize = () => {
			stop();
			initData();
			start();
		};

		window.addEventListener("resize", handleResize);

		onUnmounted(() => {
			stop();
			darkModeQuery.removeEventListener("change", handleThemeChange);
			window.removeEventListener("resize", handleResize);
		});
	});

	onUnmounted(() => {
		stop();
	});
</script>

<style scoped>
	.animated-gradient-background {
		position: fixed;
		top: 0;
		left: 0;
		width: 100vw;
		height: 100vh;
		z-index: -1;
		overflow: hidden;
		will-change: background, opacity;
		transform: translateZ(0);
		backface-visibility: hidden;
	}
</style>
