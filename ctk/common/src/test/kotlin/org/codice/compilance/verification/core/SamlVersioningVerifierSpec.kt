/*
Copyright (c) 2018 Codice Foundation

Released under the GNU Lesser General Public License; see
http://www.gnu.org/licenses/lgpl.html
*/
package org.codice.compilance.verification.core

import io.kotlintest.forAll
import io.kotlintest.matchers.string.shouldContain
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec
import org.codice.compliance.Common.Companion.buildDom
import org.codice.compliance.SAMLComplianceException
import org.codice.compliance.SAMLCore_4_1_3_2_a
import org.codice.compliance.SAMLCore_4_1_3_2_b
import org.codice.compliance.utils.ASSERTION_NAMESPACE
import org.codice.compliance.utils.PROTOCOL_NAMESPACE
import org.codice.compliance.verification.core.SamlVersioningVerifier
import java.time.Instant

class SamlVersioningVerifierSpec : StringSpec() {
    init {
        val now = Instant.now()
        val response = {
            resVersion: String, resNamespace: String?, version: String, namespace: String? ->
            """
            |<s:Response xmlns:s="$resNamespace" ID="id" Version="$resVersion" IssueInstant="$now">
            |  <s:Status>
            |    <s:StatusCode Value="urn:oasis:names:tc:SAML:2.0:status:Success"/>
            |  </s:Status>
            |  <s2:Assertion xmlns:s2="$namespace" ID="id" IssueInstant="$now" Version="$version">
            |    <s2:Issuer>https://localhost:8993/services/idp/login</s2:Issuer>
            |  </s2:Assertion>
            |</s:Response>
           """.trimMargin()
        }

        val incorrectVersions = listOf("1.0", "1.1", "3.0", "2")
        val incorrectNamespaces = listOf("incorrect_SAML_namespace",
            "urn:oasis:names:tc:SAML:1.0:protocol", "urn:oasis:names:tc:SAML:3.0:protocol")

        "response with incorrect versions fails" {
            forAll(incorrectVersions) { version ->
                buildDom(
                    response(version, PROTOCOL_NAMESPACE, "2.0", ASSERTION_NAMESPACE)).let {
                    shouldThrow<SAMLComplianceException> {
                        SamlVersioningVerifier(it).verify()
                    }
                }
            }
        }

        "response with correct version should pass" {
            buildDom(
                response("2.0", PROTOCOL_NAMESPACE, "2.0", ASSERTION_NAMESPACE)).let {
                SamlVersioningVerifier(it).verify()
            }
        }

        "response with version 1.0 should fail with SAMLCore_4_1_3_2_a" {
            buildDom(
                response("1.0", PROTOCOL_NAMESPACE, "1.0", ASSERTION_NAMESPACE)).let {
                shouldThrow<SAMLComplianceException> {
                    SamlVersioningVerifier(it).verify()
                }.message?.shouldContain(SAMLCore_4_1_3_2_a.message)
            }
        }

        "response with version 3.0 should fail with SAMLCore_4_1_3_2_b" {
            buildDom(
                response("3.0", PROTOCOL_NAMESPACE, "3.0", ASSERTION_NAMESPACE)).let {
                shouldThrow<SAMLComplianceException> {
                    SamlVersioningVerifier(it).verify()
                }.message?.shouldContain(SAMLCore_4_1_3_2_b.message)
            }
        }

        "response with incorrect assertion versions fails" {
            forAll(incorrectVersions) { version ->
                buildDom(
                    response("2.0", PROTOCOL_NAMESPACE, version, ASSERTION_NAMESPACE)).let {
                    shouldThrow<SAMLComplianceException> {
                        SamlVersioningVerifier(it).verify()
                    }
                }
            }
        }

        "response with correct namespace URIs passes" {
            buildDom(
                response("2.0", PROTOCOL_NAMESPACE, "2.0", ASSERTION_NAMESPACE)).let {
                SamlVersioningVerifier(it).verify()
            }
        }

        "response with incorrect namespace URIs fails" {
            forAll(incorrectNamespaces) { uri ->
                buildDom(
                    response("2.0", uri, "2.0", ASSERTION_NAMESPACE)).let {
                    shouldThrow<SAMLComplianceException> {
                        SamlVersioningVerifier(it).verify()
                    }
                }
            }
        }

        "response with incorrect namespace URIs on the response element fails" {
            forAll(incorrectNamespaces) { uri ->
                buildDom(
                    response("2.0", PROTOCOL_NAMESPACE, "2.0", uri)).let {
                    shouldThrow<SAMLComplianceException> {
                        SamlVersioningVerifier(it).verify()
                    }
                }
            }
        }
    }
}
