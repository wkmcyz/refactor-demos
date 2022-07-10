
// 请求返回的对象结构
interface Response {
    // 返回本次请求是否是成功的请求， 即 200
    fun isSuccessful(): Boolean

    // 返回本次请求返回码是否是 403 ，即需要认证
    fun is403(): Boolean

    fun convertToCdnInfos(): ArrayList<CdnInfo>
}

// 包含了下载所需的信息
class DownloadInfo

// 包含了使用 cdn 所需的信息
class CdnInfo


// 下载类的抽象
interface IDownloader {
    // 下载指定的资源.
    // 如果下载成功,返回 true , 否则返回 false
    fun download(info: DownloadInfo): Boolean
}

interface AuthService {
    fun updateAuth(): Response
}

interface CdnService {
    fun download(): Response
}