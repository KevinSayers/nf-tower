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
import { ActivatedRoute, Router, NavigationEnd, Params } from '@angular/router';
import {Component, EventEmitter, Input, OnChanges, OnDestroy, OnInit, Output, SimpleChanges} from '@angular/core';
import { map } from 'rxjs/operators';
import Plotly from './plotly-1.34.0.min.js';


@Component({
    selector: 'wt-reporting',
    templateUrl: './reporting.component.html',
    styleUrls: ['./reporting.component.scss']
  })

  


export class ReportingComponent implements OnInit {
    user: User;
    workflows: Workflow[];
    uniqueWfs: any[];
    runCounts: number[];
    runsAndCounts: Map<string, number>;
    startDates: any[];
    monthTotals: Map<string, number>;
    
    
    


    constructor(private httpClient: HttpClient,
      private authService: AuthService,
      private workflowService: WorkflowService,
      private router: Router,
      private route: ActivatedRoute) {}
  
      ngOnInit() {
        this.authService.user$.subscribe(
          (user: User) => {
            this.user = user;
            if (!this.user) {
              return;
            }
    
            this.workflowService.workflows$.subscribe((workflows: Workflow[]) => {
              this.receiveWorkflows(workflows);
              this.uniqueWfs = this.unique();
              this.runsAndCounts = this.countRuns();
              this.startDates = this.runDates();
              this.monthTotals = this.dateCounts();
              this.plotMonths();
            });
          }
        );
      }



      private receiveWorkflows(emittedWorkflows: Workflow[]): void {
        this.workflows = emittedWorkflows;  
      }
      get isWorkflowsInitiatied(): boolean {
        return (this.workflows != undefined);
      }

      private unique(): any[] {
        return Array.from(new Set(this.listProjects()));
      }

      private countRuns(): Map<string, number>{
        let projects = this.listProjects();
        let map = new Map<string, number>();

        this.unique().forEach( function(value){
          let count = projects.filter(x => x === value).length;
          map.set(value, count);
        })

        return map;
      }
      private listProjects(): any[] {
        var temp = [];
        this.workflows.forEach( function(value){
          temp.push(value.data.projectName)
        });
        return temp;
      }
      private runDates(): any[]{
        var dates = [];

        this.workflows.forEach( function(value){
          var temp = new Date(value.data.start)
          dates.push(temp.getMonth())
        });

        return dates;
      }

      private dateCounts(): Map<string, number>{
        var counting = new Map<string, number>();
        var uniqueMonths = Array.from(new Set(this.runDates()));
        var starts = this.startDates;

        uniqueMonths.forEach(function(value){
          let count = starts.filter(x => x === value).length;
          counting.set(value, count);
          
        })

        return counting;  
      }

      private plotMonths(): void{
        var monthNames = [ "January", "February", "March", "April", "May", "June", 
        "July", "August", "September", "October", "November", "December" ];
        
        var xs = [];
        var ys = [];
        var temp = this.monthTotals;

        temp.forEach(
          (val,key) => {
            ys.push(val);
            xs.push(monthNames[key]);
          }
        );

        var plot = [{
          x: xs,
          y: ys,
          title: "Monthly workflow submissions",
          type: 'bar'
        }];

        Plotly.newPlot('usageDiv', plot);


      }




}
    
