# shellcheck shell=ash

baseDir="$(dirname "$(readlink -f "$0")")"
# shellcheck disable=SC1091
[ -f "$baseDir/nga-utils.sh" ] && . "$baseDir/nga-utils.sh" || exit

# code before boot completed

until_boot

# code after boot completed and before unlocked

# run boot-completed.sh if it exists and root is not supported by KernelSU or APatch, then other code in this script will not run
magisk_run_completed "$baseDir"

until_unlock

# code after unlocked
nohup_bin "$baseDir/YuanShen"