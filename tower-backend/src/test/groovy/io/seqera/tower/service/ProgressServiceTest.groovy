/*
 * Copyright (c) 2019, Seqera Labs.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * This Source Code Form is "Incompatible With Secondary Licenses", as
 * defined by the Mozilla Public License, v. 2.0.
 */

package io.seqera.tower.service

import javax.inject.Inject

import grails.gorm.transactions.TransactionService
import grails.gorm.transactions.Transactional
import groovy.json.JsonSlurper
import io.micronaut.test.annotation.MicronautTest
import io.seqera.tower.Application
import io.seqera.tower.domain.Workflow
import io.seqera.tower.enums.TaskStatus
import io.seqera.tower.exchange.progress.ProcessProgress
import io.seqera.tower.exchange.progress.ProgressData
import io.seqera.tower.exchange.progress.WorkflowProgress
import io.seqera.tower.util.AbstractContainerBaseTest
import io.seqera.tower.util.DomainCreator
import io.seqera.tower.util.DomainHelper

@MicronautTest(application = Application.class)
@Transactional
class ProgressServiceTest extends AbstractContainerBaseTest {

    @Inject
    ProgressService progressService

    @Inject TransactionService tx

    void "compute simple progress" () {
        given:
        def creator = new DomainCreator()

        String process1 = 'process1'
        Workflow wf = creator.createWorkflow()
        creator.createProcess(workflow: wf, name:process1, position:0)
        creator.createTask(
                workflow: wf,
                status: TaskStatus.COMPLETED,
                process: process1,
                cpus: 1,
                realtime: 2,
                peakRss: 1,
                memory: 2,
                rchar: 3,
                wchar: 4,
                pcpu: 25,
                volCtxt: 5,
                invCtxt: 6
        )

        when: "compute the progress of the workflow"
        def progress = progressService.fetchWorkflowProgress(wf)
        then:
        with(progress.workflowProgress) {
            pending==0
            running==0
            submitted==0
            succeeded==1
            failed==0
            cached==0
            totalCpus == 1
            cpuTime == 2
            cpuLoad == 0.5f
            memoryRss == 1
            memoryReq == 2
            readBytes == 3
            writeBytes == 4
            volCtxSwitch == 5
            invCtxSwitch == 6
            cpuEfficiency == 25.0F
            memoryEfficiency == 50.0F
        }

        progress.processesProgress.size() ==1
        with(progress.processesProgress[0])  {
            pending==0
            running==0
            submitted==0
            succeeded==1
            failed==0
            cached==0
            totalCpus == 1
            cpuTime == 2
            cpuLoad == 0.5f
            memoryRss == 1
            memoryReq == 2
            readBytes == 3
            writeBytes == 4
            volCtxSwitch == 5
            invCtxSwitch == 6
        }
    }


    void "compute return process with no tasks" () {
        given:
        def creator = new DomainCreator()

        Workflow wf = creator.createWorkflow()
        creator.createProcess(workflow: wf, name:'p1', position:0)
        creator.createProcess(workflow: wf, name:'p2', position:1)

        when: "compute the progress of the workflow"
        def progress = progressService.fetchWorkflowProgress(wf)
        then:
        with(progress.workflowProgress) {
            pending==0
            running==0
            submitted==0
            succeeded==0
            failed==0
            cached==0
            totalCpus == 0
            cpuTime == 0
            cpuLoad == 0
            memoryRss == 0
            memoryReq == 0
            readBytes == 0
            writeBytes == 0
            volCtxSwitch == 0
            invCtxSwitch == 0
            cpuEfficiency == 0
            memoryEfficiency == 0
        }

        progress.processesProgress.size() ==2
        and:
        with(progress.processesProgress[0])  {
            process == 'p1'
            pending==0
            running==0
            submitted==0
            succeeded==0
            failed==0
            cached==0
        }
        and:
        with(progress.processesProgress[1])  {
            process == 'p2'
            pending==0
            running==0
            submitted==0
            succeeded==0
            failed==0
            cached==0
        }
    }

    void "compute the progress info of a workflow"() {
        given: 'create a pending task of a process and associated with a workflow (with some stats)'
        DomainCreator creator = new DomainCreator()
        Workflow workflow = creator.createWorkflow()

        String process1 = 'process1'
        String process2 = 'process2'
        creator.createProcess(name:process1, position: 0, workflow: workflow)
        creator.createProcess(name:process2, position: 1, workflow: workflow)

        and:
        creator.createTask(workflow: workflow, status: TaskStatus.NEW, process: process1, cpus: 1, realtime: 2, peakRss: 1, rchar: 1, wchar: 1, pcpu: 10, memory: 2)

        and: 'a task for the previous process in each status (with some stats each one)'
        creator.createTask(status: TaskStatus.SUBMITTED, workflow: workflow, process: process1, cpus: 1, realtime: 2, peakRss: 1, rchar: 1, wchar: 1, pcpu: 10, memory: 2)
        creator.createTask(status: TaskStatus.CACHED, workflow: workflow, process: process1, cpus: 1, realtime: 2, peakRss: 1, rchar: 1, wchar: 1, pcpu: 10, memory: 2)
        creator.createTask(status: TaskStatus.RUNNING, workflow: workflow, process: process1, cpus: 1, realtime: 2, peakRss: 1, rchar: 1, wchar: 1, pcpu: 10, memory: 2)
        creator.createTask(status: TaskStatus.FAILED, workflow: workflow, process: process1, cpus: 1, realtime: 2, peakRss: 1, rchar: 1, wchar: 1, pcpu: 10, memory: 2)
        creator.createTask(status: TaskStatus.COMPLETED, workflow: workflow, process: process1, cpus: 1, realtime: 2, peakRss: 1, rchar: 1, wchar: 1, pcpu: 10, memory: 2)

        and: 'a pending task of another process (without stats)'

        creator.createTask(status: TaskStatus.NEW, workflow: workflow, process: process2)

        and: 'a task for the previous process in each status (with some stats each one)'
        creator.createTask(status: TaskStatus.SUBMITTED, workflow: workflow, process: process2, cpus: 1, realtime: 2, peakRss: 1, rchar: 1, wchar: 1, pcpu: 10, memory: 2, volCtxt: 10, invCtxt: 30)
        creator.createTask(status: TaskStatus.CACHED, workflow: workflow, process: process2, cpus: 1, realtime: 2, peakRss: 1, rchar: 1, wchar: 1, pcpu: 10, memory: 2, volCtxt: 10, invCtxt: 30)
        creator.createTask(status: TaskStatus.RUNNING, workflow: workflow, process: process2, cpus: 1, realtime: 2, peakRss: 1, rchar: 1, wchar: 1, pcpu: 10, memory: 2, volCtxt: 10, invCtxt: 30)
        creator.createTask(status: TaskStatus.FAILED, workflow: workflow, process: process2, cpus: 1, realtime: 2, peakRss: 1, rchar: 1, wchar: 1, pcpu: 10, memory: 2, volCtxt: 10, invCtxt: 30)

        and: 'two more completed tasks (with some stats each one)'
        creator.createTask(status: TaskStatus.COMPLETED, workflow: workflow, process: process2, cpus: 1, realtime: 2, peakRss: 1, rchar: 1, wchar: 1, pcpu: 10, memory: 2, volCtxt: 10, invCtxt: 30)
        creator.createTask(status: TaskStatus.COMPLETED, workflow: workflow, process: process2, cpus: 1, realtime: 2, peakRss: 1, rchar: 1, wchar: 1, pcpu: 10, memory: 2, volCtxt: 10, invCtxt: 30)

        when: "compute the progress of the workflow"
        ProgressData progress = progressService.fetchWorkflowProgress(workflow)

        then: "the tasks has been successfully computed"
        progress.workflowProgress.pending == 2
        progress.workflowProgress.submitted == 2
        progress.workflowProgress.running == 2
        progress.workflowProgress.cached == 2
        progress.workflowProgress.failed == 2
        progress.workflowProgress.succeeded == 3

        progress.workflowProgress.totalCpus == 3
        progress.workflowProgress.cpuTime == 6
        progress.workflowProgress.cpuLoad == 0.6f
        progress.workflowProgress.memoryRss == 3
        progress.workflowProgress.memoryReq == 6
        progress.workflowProgress.readBytes == 3
        progress.workflowProgress.writeBytes == 3
        progress.workflowProgress.volCtxSwitch == 20
        progress.workflowProgress.invCtxSwitch == 60

        progress.workflowProgress.cpuEfficiency == 10.0f
        progress.workflowProgress.memoryEfficiency == 50.0f

        progress.workflowProgress.loadTasks == 2
        progress.workflowProgress.loadCpus == 2
        progress.workflowProgress.loadMemory == 4

        progress.workflowProgress.peakLoadTasks == 2
        progress.workflowProgress.peakLoadCpus == 2
        progress.workflowProgress.peakLoadMemory == 4



        then: "the processes progress has been successfully computed"
        progress.processesProgress.size() == 2
        ProcessProgress progress1 = progress.processesProgress.find { it.process == process1 }
        progress1.running == 1
        progress1.submitted == 1
        progress1.failed == 1
        progress1.pending == 1
        progress1.succeeded == 1
        progress1.cached == 1

        progress1.cpuTime == 12
        progress1.cpuLoad == 1.2f
        progress1.totalCpus == 6
        progress1.memoryRss == 6
        progress1.readBytes == 6
        progress1.writeBytes == 6
        progress1.volCtxSwitch == 0
        progress1.invCtxSwitch == 0

        ProcessProgress progress2 = progress.processesProgress.find { it.process == process2 }
        progress2.running == 1
        progress2.submitted == 1
        progress2.failed == 1
        progress2.pending == 1
        progress2.succeeded == 2
        progress2.cached == 1

        progress2.cpuTime == 12
        progress2.cpuLoad == 1.2f
        progress2.totalCpus == 6
        progress2.memoryRss == 6
        progress2.memoryReq == 12
        progress2.readBytes == 6
        progress2.writeBytes == 6
        progress2.volCtxSwitch == 60
        progress2.invCtxSwitch == 180
    }

    void "should compute load and save peak" () {
        given:
        def creator = new DomainCreator()
        def workflow = new DomainCreator().createWorkflow()

        creator.createProcess(workflow: workflow, name: 'p1', position: 0)

        creator.createTask(
                workflow: workflow,
                status: TaskStatus.RUNNING,
                process: 'p1',
                cpus: 1,
                memory: 2,
        )

        when: "compute the progress of the workflow"
        def progress = progressService.fetchWorkflowProgress(workflow)
        then:
        progress.workflowProgress.loadTasks == 1
        progress.workflowProgress.loadCpus == 1
        progress.workflowProgress.loadMemory == 2
        and:
        workflow.peakLoadTasks == 1
        workflow.peakLoadCpus == 1
        workflow.peakLoadMemory == 2

        when: 'create another running task'
        def task2 = creator.createTask(
                workflow: workflow,
                status: TaskStatus.RUNNING,
                process: 'p1',
                cpus: 10,
                memory: 40,
        )

        progress = tx.withNewTransaction {progressService.fetchWorkflowProgress(workflow)}
        then:
        progress.workflowProgress.loadTasks == 2
        progress.workflowProgress.loadCpus == 11
        progress.workflowProgress.loadMemory == 42
        and:
        workflow.peakLoadTasks == 2
        workflow.peakLoadCpus == 11
        workflow.peakLoadMemory == 42

        when: 'task 2 complete'
        task2.status = TaskStatus.COMPLETED; task2.save(flush:true)
        progress = progressService.fetchWorkflowProgress(workflow)
        then:
        progress.workflowProgress.loadTasks == 1
        progress.workflowProgress.loadCpus == 1
        progress.workflowProgress.loadMemory == 2
        and:
        workflow.peakLoadTasks == 2
        workflow.peakLoadCpus == 11
        workflow.peakLoadMemory == 42
        progress.workflowProgress.peakLoadTasks == 2
        progress.workflowProgress.peakLoadCpus == 11
        progress.workflowProgress.peakLoadMemory == 42

    }

    def 'should serialise progress' () {
        given:
        def progress = new WorkflowProgress()
        progress.taskCount[ TaskStatus.RUNNING ] = 3L
        progress.taskCount[ TaskStatus.COMPLETED ] = 4L
        progress.loadMemory = 10
        progress.cpuTime = 30

        when:
        def json = DomainHelper.toJson(progress)
        println json
        and:
        Map map = new JsonSlurper().parseText(json)
        then:
        map.cpuTime == 30 
        map.loadMemory == 10
        map.running == 3
        map.succeeded == 4
        map.containsKey('failed')
        !map.containsKey('aborted')
        !map.containsKey('taskCount')
    }
}
