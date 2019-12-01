package com.github.stephenott.qtz.forms.submission.repository

import com.github.stephenott.qtz.forms.submission.domain.SubmissionEntity
import io.micronaut.data.annotation.Repository
import io.micronaut.data.repository.reactive.ReactiveStreamsCrudRepository
import java.util.*

@Repository
interface SubmissionRepository : ReactiveStreamsCrudRepository<SubmissionEntity, UUID> {
}