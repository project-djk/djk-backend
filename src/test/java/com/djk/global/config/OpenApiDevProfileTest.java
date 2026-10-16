package com.djk.global.config;

import org.springframework.test.context.ActiveProfiles;

/** dev 프로파일에서도 local 과 똑같이 API 문서를 노출한다. */
@ActiveProfiles(value = "dev", inheritProfiles = false)
class OpenApiDevProfileTest extends OpenApiLocalProfileTest {
}
