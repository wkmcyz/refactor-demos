/**
 *  啊,好复杂
 */
class AuthCdnDownloader : IDownloader {

    // 内部保存的 cdn 列表. ArrayList<CdnInfo> 表示 CdnInfo 数组
    // 不可改动
    private val cdnInfos = ArrayList<CdnInfo>()

    // 设置所有的 cdn.
    // 不可改动
    fun setCdnInfos(cdnInfos: ArrayList<CdnInfo>) {
        this.cdnInfos.addAll(cdnInfos)
    }

    fun setCdnService(authService: AuthService) {
        this.authService = authService
    }

    fun setAuthService(cdnService: CdnService) {
        this.cdnService = cdnService
    }

    private lateinit var authService: AuthService

    private lateinit var cdnService: CdnService

    // 不可改动
    override fun download(info: DownloadInfo): Boolean {
        return innerDownload(info, false)
    }


    private fun innerDownload(
        info: DownloadInfo,
        hasUpdateAuth: Boolean
    ): Boolean {

        for (cdnInfo in cdnInfos) {
            var response: Response = null
            try {
                response = cdnService.download()
            } catch (e: Exception) {
                // 发生 Exception，也重试一次
                continue
            }
            // 成功了可以直接返回
            if (response.isSuccessful()) {
                return true
            }
            // 失败了，且是第一次 403 才会去更新 auth
            if (response.is403() && !hasUpdateAuth) {
                needUpdateAuth = true
                break
            }
            // 其他失败情况，就换个 url 再试
        }
        if (needUpdateAuth) {
            // 更新 auth 失败，直接算 download 失败
            return if (!updateAuth()) {
                false
            } else {
                // 更新 auth 成功，用新 auth 信息再走一遍下载流程
                return innerDownload(info, true)
            }
        }
        // 都试完了都没成
        return false
    }

    private fun updateAuth(): Boolean {
        val response = try {
            authService.updateAuth()
        } catch (e: Exception) {
            return false
        }
        if (!response.isSuccessful()) return false
        cdnInfos.clear()
        cdnInfos.addAll(response.convertToCdnInfos())
        return true
    }

}
