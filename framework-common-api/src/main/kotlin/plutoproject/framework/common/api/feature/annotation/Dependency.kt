package plutoproject.framework.common.api.feature.annotation

import plutoproject.framework.common.api.feature.Load

@Target()
@Retention(AnnotationRetention.RUNTIME)
annotation class Dependency(
    val id: String,
    val load: Load = Load.BEFORE,
    val required: Boolean = true,
)
