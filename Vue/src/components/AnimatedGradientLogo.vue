<template>
	<div
		class="animated-gradient-logo"
		:style="{
			width: `${size}px`,
			height: `${size}px`,
			opacity: alpha,
			transform: `scale(${scale})`,
		}">
		<div class="logo-gradient" :style="gradientStyle"></div>
	</div>
</template>

<script setup lang="ts">
	import { computed } from "vue";
	import type { RGBColor } from "../utils/ColorUtils";
	import { rgbToCSS, deepen } from "../utils/ColorUtils";

	const props = withDefaults(
		defineProps<{
			colors: RGBColor[];
			size?: number;
			alpha?: number;
			scale?: number;
			useDeepened?: boolean;
		}>(),
		{
			size: 150,
			alpha: 1,
			scale: 1,
			useDeepened: true,
		}
	);

	const gradientStyle = computed(() => {
		let colors = props.colors;

		if (props.useDeepened && colors.length > 0) {
			colors = colors.map((color) => {
				const brightness = (color.r + color.g + color.b) / 3;

				if (brightness > 0.85) {
					return deepen(color, 0.4, 0.5);
				} else {
					return deepen(color, 0.3, 0.65);
				}
			});
		}

		const colorStops = colors.map((c) => rgbToCSS(c)).join(", ");

		return {
			background: `linear-gradient(135deg, ${colorStops})`,
			maskImage: "url(/file.svg)",
			WebkitMaskImage: "url(/file.svg)",
			maskSize: "contain",
			WebkitMaskSize: "contain",
			maskRepeat: "no-repeat",
			WebkitMaskRepeat: "no-repeat",
			maskPosition: "center",
			WebkitMaskPosition: "center",
		};
	});
</script>

<style scoped>
	.animated-gradient-logo {
		transition: opacity 0.3s ease, transform 0.3s ease;
		will-change: opacity, transform;
		transform: translateZ(0);
		backface-visibility: hidden;
	}

	.logo-gradient {
		width: 100%;
		height: 100%;
		will-change: background;
		transform: translateZ(0);
	}
</style>
