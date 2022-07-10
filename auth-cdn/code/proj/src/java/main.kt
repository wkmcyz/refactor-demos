fun main() {
    // 已有的条件:
    // 不可改动
    val cdnInfos = ArrayList<CdnInfo>()
    val authService = AuthService()
    val cdnService = CdnService()
    val downloadInfo = DownloadInfo()

    // 使用 downloader 开始下载.
    val downloader = AuthCdnDownloader()
    downloader.setCdnService(cdnService)
    downloader.setAuthService(authService)
    downloader.setCdnInfos(cdnInfos)
    val result = downloader.download(downloadInfo)
    println(result)
}