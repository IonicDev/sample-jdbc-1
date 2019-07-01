package com.ionic.sdk.addon.policy;

import com.ionic.sdk.agent.config.AgentConfig;
import com.ionic.sdk.agent.service.IDC;
import com.ionic.sdk.core.codec.Transcoder;
import com.ionic.sdk.device.DeviceUtils;
import com.ionic.sdk.error.IonicException;
import com.ionic.sdk.error.SdkData;
import com.ionic.sdk.error.SdkError;
import com.ionic.sdk.httpclient.Http;
import com.ionic.sdk.httpclient.HttpClient;
import com.ionic.sdk.httpclient.HttpClientDefault;
import com.ionic.sdk.httpclient.HttpHeader;
import com.ionic.sdk.httpclient.HttpHeaders;
import com.ionic.sdk.httpclient.HttpRequest;
import com.ionic.sdk.httpclient.HttpResponse;
import com.ionic.sdk.json.JsonIO;

import javax.json.JsonObject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class PolicyService {

    private final URL urlIonicApi;
    private final String tenantId;
    private final String user;
    private final String password;

    public PolicyService(URL urlIonicApi, String tenantId, String user, String password) {
        this.urlIonicApi = urlIonicApi;
        this.tenantId = tenantId;
        this.user = user;
        this.password = password;
    }

    /**
     * Add a new policy to the server policies associated with the tenant.
     *
     * @param policyJson Ionic policy JSON to apply to server
     * @return the server identifier for the newly created policy
     * @throws IonicException on server request failure; unrecognized response
     */
    public String addPolicy(final byte[] policyJson) throws IonicException {
        final String file = String.format(RESOURCE_POLICY_CREATE, tenantId);
        final HttpClient httpClient = new HttpClientDefault(new AgentConfig(), urlIonicApi.getProtocol());
        final String authorizationValue = String.format("%s:%s", user, password);
        final String authorization = String.format(PATTERN_AUTHORIZATION_VALUE,
                Transcoder.base64().encode(Transcoder.utf8().decode(authorizationValue)));
        final HttpHeaders httpHeaders = new HttpHeaders(
                new HttpHeader(HEADER_AUTHORIZATION, authorization),
                new HttpHeader(Http.Header.CONTENT_TYPE, Http.Header.CONTENT_TYPE_SERVER),
                new HttpHeader(HEADER_ACCEPTS, Http.Header.CONTENT_TYPE_SERVER)
        );
        final ByteArrayInputStream entityIn = new ByteArrayInputStream(policyJson);
        final HttpRequest httpRequest = new HttpRequest(urlIonicApi, Http.Method.POST, file, httpHeaders, entityIn);
        try {
            final HttpResponse httpResponse = httpClient.execute(httpRequest);
            SdkData.checkTrue(HttpURLConnection.HTTP_CREATED == httpResponse.getStatusCode(),
                    SdkError.ISAGENT_REQUESTFAILED);
            final byte[] entityOut = DeviceUtils.read(httpResponse.getEntity());
            final JsonObject jsonResponse = JsonIO.readObject(new ByteArrayInputStream(entityOut));
            return jsonResponse.getString(IDC.Payload.ID);
        } catch (IOException e) {
            throw new IonicException(SdkError.ISAGENT_REQUESTFAILED, e);
        }
    }

    /**
     * Remove a policy from the server policies associated with the tenant.
     *
     * @param policyId the server identifier for the policy to be deleted
     * @throws IonicException on server request failure
     */
    public void deletePolicy(final String policyId) throws IonicException {
        final String file = String.format(RESOURCE_POLICY_DELETE, tenantId, policyId);
        final HttpClient httpClient = new HttpClientDefault(new AgentConfig(), urlIonicApi.getProtocol());
        final String authorizationValue = String.format("%s:%s", user, password);
        final String authorization = String.format(PATTERN_AUTHORIZATION_VALUE,
                Transcoder.base64().encode(Transcoder.utf8().decode(authorizationValue)));
        final HttpHeaders httpHeaders = new HttpHeaders(
                new HttpHeader(HEADER_AUTHORIZATION, authorization),
                new HttpHeader(HEADER_ACCEPTS, Http.Header.CONTENT_TYPE_SERVER)
        );
        final HttpRequest httpRequest = new HttpRequest(urlIonicApi, METHOD_DELETE, file, httpHeaders, null);
        try {
            final HttpResponse httpResponse = httpClient.execute(httpRequest);
            SdkData.checkTrue(HttpURLConnection.HTTP_NO_CONTENT == httpResponse.getStatusCode(),
                    SdkError.ISAGENT_REQUESTFAILED);
        } catch (IOException e) {
            throw new IonicException(SdkError.ISAGENT_REQUESTFAILED, e);
        }
    }

    private static final String RESOURCE_POLICY_CREATE = "/v2/%s/policies";
    private static final String RESOURCE_POLICY_DELETE = "/v2/%s/policies/%s";
    private static final String METHOD_DELETE = "DELETE";
    private static final String HEADER_ACCEPTS = "Accepts";
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String PATTERN_AUTHORIZATION_VALUE = "Basic %s";
}
