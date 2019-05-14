package io.seqera.watchtower.domain

import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.databind.ObjectMapper
import grails.gorm.annotation.Entity
import groovy.transform.CompileDynamic
import io.seqera.watchtower.pogo.enums.TaskStatus

import java.time.Instant

/**
 * Workflow task info.
 * @see https://www.nextflow.io/docs/latest/tracing.html#execution-report
 */
@Entity
@JsonIgnoreProperties(['dirtyPropertyNames', 'errors', 'dirty', 'attached', 'version'])
@CompileDynamic
class Task {

    static belongsTo = [workflow: Workflow]
    String workflowId

    /**
     * The order of the task in the workflow
     */
    Long taskId
    String hash
    String name
    String process
    String tag

    TaskStatus status

    Instant submit
    Instant start
    Instant complete

    /**
     * Multi-value field encoded as JSON
     */
    String module

    String container
    Integer attempt
    String script
    String scratch
    String workdir

    String queue
    Integer cpus
    Long memory
    Long disk
    String time
    String env

    String errorAction

    Long exit
    Long duration
    Long realtime
    Long nativeId

    Double pcpu
    Double pmem
    Long rss
    Long vmem
    Long peakRss
    Long peakVmem
    Long rchar
    Long wchar
    Long syscr
    Long syscw
    Long readBytes
    Long writeBytes

    Long volCtxt
    Long invCtxt

    boolean checkIsSubmitted() {
        status == TaskStatus.SUBMITTED
    }

    boolean checkIsRunning() {
        status == TaskStatus.RUNNING
    }

    boolean checkIsSucceeded() {
        (status == TaskStatus.COMPLETED) && !errorAction
    }

    boolean checkIsFailed() {
        (status == TaskStatus.COMPLETED) && errorAction
    }

    @JsonSetter('submit')
    void deserializeSubmitInstant(Long submitEpoch) {
        submit = submitEpoch ? Instant.ofEpochMilli(submitEpoch) : null
    }

    @JsonSetter('start')
    void deserializeStartInstant(Long startEpoch) {
        start = startEpoch ? Instant.ofEpochMilli(startEpoch) : null
    }

    @JsonSetter('complete')
    void deserializeCompleteInstant(Long completeEpoch) {
        complete = completeEpoch ? Instant.ofEpochMilli(completeEpoch) : null
    }

    @JsonSetter('module')
    void deserializeModuleJson(List<String> moduleList) {
        module = moduleList ? new ObjectMapper().writeValueAsString(moduleList) : null
    }


    @JsonGetter('submit')
    Long serializeSubmitInstant() {
        submit?.toEpochMilli()
    }

    @JsonGetter('start')
    Long serializeStartInstant() {
        start?.toEpochMilli()
    }

    @JsonGetter('complete')
    Long serializeCompleteInstant() {
        complete?.toEpochMilli()
    }

    static transients = ['workflowId']

    static mapping = {
        version false
    }

    static constraints = {
        taskId(unique: 'workflow')

        process(nullable: true)
        tag(nullable: true)
        exit(nullable: true)
        start(nullable: true)
        complete(nullable: true)
        module(nullable: true)
        container(nullable: true)
        attempt(nullable: true)
        script(nullable: true)
        scratch(nullable: true)
        workdir(nullable: true)
        queue(nullable: true)
        cpus(nullable: true)
        memory(nullable: true)
        disk(nullable: true)
        time(nullable: true)
        env(nullable: true)
        errorAction(nullable: true)
        duration(nullable: true)
        realtime(nullable: true)
        nativeId(nullable: true)
        pcpu(nullable: true)
        pmem(nullable: true)
        rss(nullable: true)
        vmem(nullable: true)
        peakRss(nullable: true)
        peakVmem(nullable: true)
        rchar(nullable: true)
        wchar(nullable: true)
        syscr(nullable: true)
        syscw(nullable: true)
        readBytes(nullable: true)
        writeBytes(nullable: true)
        volCtxt(nullable: true)
        invCtxt(nullable: true)
    }

}