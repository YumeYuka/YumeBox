# shellcheck shell=ash

# shellcheck disable=SC1091,SC2015
[ -f "$MODPATH/nga-utils.sh" ] && . "$MODPATH/nga-utils.sh" || abort '! File "nga-utils.sh" does not exist!'
nga_install_init # Don't write code before this line!

newline 2
pure_print "泠熙子酱●█▀█▄泠熙子酱●█▀█▄泠熙子酱●█▀█▄泠熙子酱●█▀█▄泠熙子酱●█▀█▄泠熙子酱●█▀█▄泠熙子酱●█▀█▄泠熙子酱●█▀█▄泠熙子酱●█▀█▄泠熙子酱●█▀█▄泠熙子酱●█▀█▄泠熙子酱●█▀█▄泠熙子酱●█▀█▄泠熙子酱●█▀█▄泠熙子酱●█▀█▄泠熙子酱●█▀█▄泠熙子酱●█▀█▄泠熙子酱●█▀█▄泠熙子酱●█▀█▄泠熙子酱●█▀█▄泠熙子酱●█▀█▄泠熙子酱●█▀█▄"
newline 2
pure_print "你说得对，但是《原神》是由米哈游自主研发的 一款全新开放世界冒险游戏。游戏发生在一个被 称作“提瓦特”的幻想世界，在这里，被神选中的 人将被授予“神之眼”，导引元素之力。你将扮演 一位名为“旅行者”的神秘角色，在自由的旅行中 邂逅性格各异、能力独特的同伴们，和他们一起 击败强敌，找回失散的亲人——同时，逐步发掘 “原神”的真相"
newline 2
pure_print "模块安装完成 !!! "
pure_print "了解源码请查看 https://github.com/YumeYuka/YuanShen"
newline 
pure_print "按下音量下键跳转浏览器进入Github地址(其他任意键取消)"
[ $(until_key) = down ] && {
  pure_print "已跳转"
  goto_url 'https://github.com/YumeYuka/YuanShen'
  true
} || pure_print "已取消跳转"


nga_install_done # Don't write code after this line!
