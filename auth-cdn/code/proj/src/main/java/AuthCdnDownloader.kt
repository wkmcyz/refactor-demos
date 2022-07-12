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
        val tryDownloadResult = tryDownload(info)
        return when (tryDownloadResult) {
            TryDownloadResult.SUCCESS -> {
                true
            }
            TryDownloadResult.AUTH_FAILED -> {
                reAuthAndDownload(info)
            }
            TryDownloadResult.TRY_NEXT_CDN, null -> {
                false
            }
        }
    }

    enum class TryDownloadResult {
        AUTH_FAILED,
        SUCCESS,
        TRY_NEXT_CDN
    }

    private fun onceDownload(cdnInfo: CdnInfo, info: DownloadInfo): TryDownloadResult {
        var tryDownloadResult: TryDownloadResult = null
        var response: Response = null
        try {
            response = cdnService.download()
        } catch (e: Exception) {
            // 发生 Exception，也重试一次
            tryDownloadResult = TryDownloadResult.TRY_NEXT_CDN
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

    private fun tryDownload(info: DownloadInfo): TryDownloadResult? {
        return this.cdnInfos.mapUntil(
            { cdnInfo ->
                onceDownload(cdnInfo, info)
            },
            { result ->
                result != TryDownloadResult.TRY_NEXT_CDN
            },
        )
    }

    private fun reAuthAndDownload(info: DownloadInfo): Boolean {
        val updateAuthResult = updateAuth()
        if (!updateAuthResult) {
            return false
        }
        val secondTryDownloadResult = tryDownload(info)
        return secondTryDownloadResult == TryDownloadResult.SUCCESS
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
