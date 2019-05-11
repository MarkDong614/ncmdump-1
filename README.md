Netease Cloud Music Copyright Protection File Dump
===========

## 简介
此项目为[ncmdump](https://github.com/anonymous5l/ncmdump)的Java移植版本，核心代码在`NcmDump.java`里面，
界面使用javafx开发。

## 依赖库
	jaudiotagger
	gson

## 代码
界面相关类：MainUI, TaskBean TaskStatus
破解相关类: NcmDump, NcmFile, ID3Data
直接调用`NcmDump.dump(ncmFile, outDirectory)`执行破解

## 使用
- 程序已经打包好[(这里下载)](https://github.com/Yeamy/ncmdump/releases)，使用前请先安装JRE（废话）

- 生成文件默认与源文件同目录，点击`输出目录`按钮即可修改，再次点击会恢复成与源文件同目录，输出目录在转换期间不能修改。

- 拖动.ncm文件至界面会直接执行转换，相同名字文件会被新文件覆盖，转换期间不能添加文件。

## 说明
- 目前移植的版本已经参考最新版更新，以后会不定时更新。

- 代码已经支持.flac文件的ID3写入，能否读取取决于您的播放器。

- 专辑封面支持jpg、png格式图片，

- 兼容.ncm文件歌曲信息不存在的情况（原作者提示，存在.ncm没有携带ID3数据的情况）

- 目前手头上没有特殊文件可测试，所以将就着用吧