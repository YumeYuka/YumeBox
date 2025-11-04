# shellcheck shell=ash

baseDir="$(dirname "$(readlink -f "$0")")"
# shellcheck disable=SC1091
[ -f "$baseDir/nga-utils.sh" ] && . "$baseDir/nga-utils.sh" || exit

am start -n com.miHoYo.Yuanshen/com.miHoYo.GetMobileInfo.MainActivity
