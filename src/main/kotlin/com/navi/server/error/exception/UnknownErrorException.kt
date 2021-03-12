package com.navi.server.error.exception

import java.lang.RuntimeException

class UnknownErrorException(message: String) : RuntimeException(message)