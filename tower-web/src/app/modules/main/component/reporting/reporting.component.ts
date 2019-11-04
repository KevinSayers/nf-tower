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

import {User} from "../../entity/user/user";
import {HttpClient} from "@angular/common/http";
import {Workflow} from "src/app/modules/main/entity/workflow/workflow";
import {WorkflowService} from "src/app/modules/main/service/workflow.service";
import {AuthService} from "src/app/modules/main/service/auth.service";
import {NotificationService} from "src/app/modules/main/service/notification.service";
import { ActivatedRoute, Router, NavigationEnd, Params } from '@angular/router';
import {debounceTime, distinctUntilChanged, filter} from 'rxjs/operators';
import {FormControl} from "@angular/forms";
import {FilteringParams} from "../../util/filtering-params";
import {Component, EventEmitter, Input, OnChanges, OnDestroy, OnInit, Output, SimpleChanges} from '@angular/core';


@Component({
    selector: 'wt-reporting',
    templateUrl: './reporting.component.html',
    styleUrls: ['./reporting.component.scss']
  })

export class ReportingComponent implements OnInit {
    user: User;
    @Input()
    workflows: Workflow[];
  

    constructor(private authService: AuthService) {
  
    }
  
    ngOnInit() {
      this.authService.user$.subscribe(
        (user: User) => this.user = user
      )
    }
}