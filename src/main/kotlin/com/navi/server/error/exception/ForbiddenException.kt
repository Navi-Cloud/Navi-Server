package com.navi.server.error.exception

import java.lang.RuntimeException

class ForbiddenException(message: String) : RuntimeException(message)