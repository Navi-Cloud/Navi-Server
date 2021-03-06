package com.navi.server.error.exception

import java.lang.RuntimeException

class NotFoundException(message: String) : RuntimeException(message)