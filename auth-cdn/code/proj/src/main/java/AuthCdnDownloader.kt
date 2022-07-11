import java.util.*


fun <T, U> ArrayList<T>.mapUntil(map: ((T) -> U), predict: ((U) -> Boolean)): U? {
    var result: U? = null
    for (t in this) {
        result = map(t)
        if (predict(result)) {
            return result
        }
    }
    return null
}


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
        return innerDownload(info)
    }

    enum class TryDownloadResult {
        AUTH_FAILED,
        SUCCESS,
        FAILED
    }

    private fun onceDownload(cdnInfo: CdnInfo, info: DownloadInfo): TryDownloadResult {
        var tryDownloadResult: TryDownloadResult = null
        var response: Response = null
        try {
            response = cdnService.download()
        } catch (e: Exception) {
            // 发生 Exception，也重试一次
            tryDownloadResult = TryDownloadResult.FAILED
        }
        // 成功了可以直接返回
        if (response.isSuccessful()) {
            tryDownloadResult = TryDownloadResult.SUCCESS
        }
        // 失败了，且是第一次 403 才会去更新 auth
        else if (response.is403()) {
            tryDownloadResult = TryDownloadResult.AUTH_FAILED
        }
        return tryDownloadResult
    }


    private fun innerDownload(
        info: DownloadInfo
    ): Boolean {
        val tryDownloadResult = this.cdnInfos.mapUntil(
            { cdnInfo ->
                onceDownload(cdnInfo, info)
            },
            { result ->
                result != TryDownloadResult.FAILED
            },
        )
        if (tryDownloadResult == TryDownloadResult.SUCCESS) {
            return true
        } else if (tryDownloadResult == TryDownloadResult.AUTH_FAILED) {
            // 更新 auth 失败，直接算 download 失败
            val updateAuthResult = updateAuth()
            if (!updateAuthResult) {
                return false
            }
            val tryDownloadResult = this.cdnInfos.mapUntil(
                { cdnInfo ->
                    onceDownload(cdnInfo, info)
                },
                { result ->
                    result != TryDownloadResult.FAILED
                },
            )
            return tryDownloadResult == TryDownloadResult.SUCCESS
        } else {
            // 都试完了都没成
            return false
        }
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
