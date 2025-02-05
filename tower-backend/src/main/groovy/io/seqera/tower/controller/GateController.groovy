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

package io.seqera.tower.controller

import javax.inject.Inject
import javax.mail.MessagingException
import javax.validation.ValidationException

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.seqera.tower.exchange.gate.AccessGateRequest
import io.seqera.tower.exchange.gate.AccessGateResponse
import io.seqera.tower.service.GateService

/**
 * Manages the access/sign-in requests to Tower
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */

@Slf4j
@Controller("/gate")
class GateController extends BaseController {

    @Inject
    GateService gate

    @Post("/access")
    @Secured(SecurityRule.IS_ANONYMOUS)
    HttpResponse<AccessGateResponse> access(AccessGateRequest request) {
        try {
            if( !request.email )
                return HttpResponse.badRequest(new AccessGateResponse(message: "Oops.. Missing registration email"))

            final result = gate.access(request.email)
            assert result.user
            assert result.state
            return HttpResponse.ok(result)
        }
        catch (ValidationException e) {
            return HttpResponse.badRequest(new AccessGateResponse(message: e.message))
        }
        catch (MessagingException e) {
            final msg = "Mailing error: ${e.message}"
            log.error(msg, e)
            return HttpResponse.badRequest(new AccessGateResponse(message: msg))
        }

        catch (Exception e) {
            final msg = "Oops.. Something went wrong during the registration procedure"
            log.error(msg, e)
            return HttpResponse.badRequest(new AccessGateResponse(message: msg))
        }
    }
}
