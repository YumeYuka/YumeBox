<template>
	<div class="home-screen">
		<AnimatedGradientBackground :alpha="1" @color-update="handleColorUpdate" />
		<div class="content-layer">
			<div class="logo-section">
				<AnimatedGradientLogo :colors="[logoColor]" :size="150" :alpha="1" :scale="1" :useDeepened="false" />
				<h1 class="title-text" :style="{ color: logoColorCSS }">Genshin Impact</h1>
				<p class="version-text">{{ genshinVersion }}</p>
			</div>
			<div class="spacer"></div>
			<div class="info-card">
				<div class="card-title">{{ deviceName }}</div>
				<div class="card-bottom">
					<div class="card-item">
						<div class="card-value">{{ daysToNextVersion }}</div>
						<div class="card-label">版本倒计时</div>
					</div>
				</div>
				<div class="card-bottom">
					<div class="card-item">
						<div class="card-value">{{ systemVersion }}</div>
						<div class="card-label">系统版本</div>
					</div>
				</div>
			</div>
			<button class="start-button" @click="handleStartGame">原神·启动</button>
		</div>
	</div>
</template>

<script setup lang="ts">
	import { ref, onMounted, onUnmounted, computed } from "vue";
	import AnimatedGradientBackground from "./AnimatedGradientBackground.vue";
	import AnimatedGradientLogo from "./AnimatedGradientLogo.vue";
	import { useGradientController } from "../utils/useGradientController";
	import type { RGBColor } from "../utils/ColorUtils";
	import { exec, toast, fullScreen, type ExecResult } from "kernelsu";

	const controller = useGradientController({
		actionBarPadding: 200,
		logoPadding: 300,
		logoHeight: 120,
	});

	const genshinVersion = ref("Loading...");
	const deviceName = ref("Loading...");
	const daysToNextVersion = ref("Calculating...");
	const systemVersion = ref("Loading...");

	let animationFrameId: number | null = null;
	let lastTime = 0;

	const baseColor: RGBColor = { r: 0.604, g: 0.302, b: 0.584, a: 1 };
	const deepenFactor = 0.7;
	const logoColor = ref<RGBColor>({
		r: baseColor.r * deepenFactor,
		g: baseColor.g * deepenFactor,
		b: baseColor.b * deepenFactor,
		a: 1,
	});

	const logoColorCSS = computed(() => {
		const c = logoColor.value;
		return `rgb(${Math.round(c.r * 255)}, ${Math.round(c.g * 255)}, ${Math.round(c.b * 255)})`;
	});

	const handleColorUpdate = (_colors: RGBColor[]) => {};

	const handleStartGame = async () => {
		try {
			const result: ExecResult = await exec("am start -n com.miHoYo.Yuanshen/com.miHoYo.GetMobileInfo.MainActivity");

			if (result.errno === 0) {
				console.log("Genshin Impact launched successfully");
				toast("Launching Genshin Impact...");
			} else {
				console.error("Failed to launch Genshin Impact:", result.stderr);
				toast("Launch failed: " + result.stderr);
			}
		} catch (e) {
			console.error("Launch exception:", e);
			toast("Launch exception: " + String(e));
		}
	};

	const getGenshinInfo = async () => {
		try {
			const sdkResult: ExecResult = await exec("getprop ro.build.version.sdk");
			const sdkVersion = sdkResult.errno === 0 ? parseInt(sdkResult.stdout.trim()) : 0;

			let deviceNameResult: ExecResult;
			if (sdkVersion >= 36) {
				deviceNameResult = await exec("su -c 'getprop persist.private.device_name'");
			} else {
				deviceNameResult = await exec("getprop persist.sys.device_name");
			}

			if (deviceNameResult.errno === 0 && deviceNameResult.stdout) {
				deviceName.value = deviceNameResult.stdout.trim();
			} else {
				const modelResult: ExecResult = await exec("getprop ro.product.model");
				if (modelResult.errno === 0 && modelResult.stdout) {
					deviceName.value = modelResult.stdout.trim();
				}
			}

			const systemResult: ExecResult = await exec("getprop ro.build.version.incremental");
			if (systemResult.errno === 0 && systemResult.stdout) {
				systemVersion.value = systemResult.stdout.trim();
			}

			const genshinVersionResult: ExecResult = await exec("dumpsys package com.miHoYo.Yuanshen | grep versionName");
			if (genshinVersionResult.errno === 0 && genshinVersionResult.stdout) {
				const versionName = genshinVersionResult.stdout.split("=").pop()?.trim();

				const genshinCodeResult: ExecResult = await exec("dumpsys package com.miHoYo.Yuanshen | grep versionCode");
				const versionCode =
					genshinCodeResult.errno === 0 ? genshinCodeResult.stdout.split("=").pop()?.split(" ")[0]?.trim() : null;

				if (versionName && versionCode) {
					genshinVersion.value = `${versionName} (${versionCode})`;
				} else if (versionName) {
					genshinVersion.value = versionName;
				} else {
					genshinVersion.value = "Not Installed";
				}
			} else {
				genshinVersion.value = "Not Installed";
			}

			const version61ReleaseDate = new Date(2025, 9, 21);
			const today = new Date();
			const daysSinceRelease = Math.floor((today.getTime() - version61ReleaseDate.getTime()) / (1000 * 60 * 60 * 24));
			const daysInCycle = daysSinceRelease % 45;
			const daysRemaining = 45 - daysInCycle;
			const currentVersionNumber = 6.1 + Math.floor(daysSinceRelease / 45) * 0.1;
			const nextVersionNumber = currentVersionNumber + 0.1;
			daysToNextVersion.value = `距离 v${nextVersionNumber.toFixed(1)} 还有 ${daysRemaining} 天`;
		} catch (e) {
			console.error("Failed to load genshin info:", e);
			genshinVersion.value = "Failed";
			deviceName.value = "Failed";
			daysToNextVersion.value = "Failed";
			systemVersion.value = "Failed";
		}
	};

	const animationLoop = (currentTime: number) => {
		if (lastTime === 0) {
			lastTime = currentTime;
		}

		const deltaTime = (currentTime - lastTime) / 1000;
		lastTime = currentTime;

		controller.updateFrame(deltaTime);
		animationFrameId = requestAnimationFrame(animationLoop);
	};

	const startAnimation = () => {
		if (animationFrameId === null) {
			lastTime = 0;
			animationFrameId = requestAnimationFrame(animationLoop);
		}
	};

	const stopAnimation = () => {
		if (animationFrameId !== null) {
			cancelAnimationFrame(animationFrameId);
			animationFrameId = null;
		}
	};

	onMounted(() => {
		try {
			fullScreen(true);
		} catch (e) {
			console.error("Failed to enable fullscreen:", e);
		}

		getGenshinInfo();
		startAnimation();
	});

	onUnmounted(() => {
		stopAnimation();
	});
</script>

<style scoped>
	.home-screen {
		position: fixed;
		top: 0;
		left: 0;
		width: 100vw;
		height: 100vh;
		overflow: hidden;
	}

	.content-layer {
		position: relative;
		z-index: 1;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: flex-start;
		width: 100%;
		height: 100%;
		padding: 1rem;
		padding-top:30vh;
		box-sizing: border-box;
	}

	.logo-section {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 4px;
	}

	.title-text {
		font-size: 38px;
		font-weight: 700;
		margin: 0;
		line-height: 1.2;
	}

	.version-text {
		font-size: 13px;
		color: rgba(0, 0, 0, 0.6);
		margin: 0;
		transition: opacity 0.3s ease;
	}

	@media (prefers-color-scheme: dark) {
		.version-text {
			color: rgba(255, 255, 255, 0.6);
		}
	}

	.spacer {
		flex: 1;
		min-height: 20px;
	}

	.info-card {
		background: #ffffff;
		border-radius: 20px;
		padding: 20px 18px;
		width: 100%;
		max-width: 500px;
		border: 1px solid rgba(0, 0, 0, 0.08);
		display: flex;
		flex-direction: column;
		gap: 16px;
	}

	@media (prefers-color-scheme: dark) {
		.info-card {
			background: #ffffff;
			border: 1px solid rgba(0, 0, 0, 0.08);
		}
	}

	.card-title {
		font-size: 22px;
		font-weight: 500;
		color: rgba(0, 0, 0, 0.9);
		text-align: left;
		line-height: 1.3;
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	@media (prefers-color-scheme: dark) {
		.card-title {
			color: rgba(255, 255, 255, 0.9);
		}
	}

	.card-bottom {
		display: flex;
		justify-content: flex-start;
		gap: 32px;
	}

	.card-item {
		display: flex;
		flex-direction: column;
		gap: 2px;
		flex: 1;
	}

	.card-value {
		font-size: 14px;
		font-weight: 500;
		color: rgba(0, 0, 0, 0.9);
		line-height: 1.4;
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	@media (prefers-color-scheme: dark) {
		.card-value {
			color: rgba(255, 255, 255, 0.9);
		}
	}

	.card-label {
		font-size: 11px;
		font-weight: 400;
		color: rgba(0, 0, 0, 0.5);
		line-height: 1.4;
	}

	@media (prefers-color-scheme: dark) {
		.card-label {
			color: rgba(255, 255, 255, 0.5);
		}
	}

	.start-button {
		width: 100%;
		max-width: 500px;
		padding: 16px 32px;
		margin-top: 16px;
		margin-bottom: 32px;
		font-size: 16px;
		font-weight: 600;
		color: white;
		background: linear-gradient(135deg, #3b82f6, #2563eb);
		border: none;
		border-radius: 12px;
		cursor: pointer;
	}

	.start-button:active {
		opacity: 0.8;
	}

	@media (max-width: 768px) {
		.content-layer {
			padding: 1rem;
			padding-top: 20vh;
		}
	}
</style>
