package github.buriedincode.openlibrary

open class ServiceException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)

class CacheException(message: String? = null, cause: Throwable? = null) : ServiceException(message, cause)
