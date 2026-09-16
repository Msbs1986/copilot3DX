package com.keonys.copilot3DX.service;

import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.keonys.copilot3DX.config.HttpRequestService;
import com.keonys.copilot3DX.dto.DocumentDetailDto;
import com.keonys.copilot3DX.dto.DocumentDto;
import com.keonys.copilot3DX.model.DocumentFileInfo;
import com.keonys.copilot3DX.model.DocumentInfo;

@Service
public class DocumentService {

    private static final String DOCUMENT_SEARCH_ENDPOINT =
            "/resources/v1/modeler/documents/search";

    private static final String DOCUMENT_ENDPOINT =
            "/resources/v1/modeler/documents/";

    @Value("${threedx.space-url}")
    private String space3dsUrlStr;

    @Value("${threedx.security-context}")
    private String secContext;

    private final Service3DXConnexion service3DXConnexion;
    private final HttpRequestService httpRequestService;

    public DocumentService(
            Service3DXConnexion service3DXConnexion,
            HttpRequestService httpRequestService) {

        this.service3DXConnexion = service3DXConnexion;
        this.httpRequestService = httpRequestService;
    }

    public DocumentDto searchDocuments(
            String searchStr) throws Exception {

        prepareConnection();

        Map<String, String> headers =
                createSecurityHeaders();

        String effectiveSearchStr =
                searchStr == null || searchStr.isBlank()
                        ? "*"
                        : searchStr.trim();

        String encodedSearchStr =
                URLEncoder.encode(
                        effectiveSearchStr,
                        StandardCharsets.UTF_8);

        String searchUrl =
                normalizeBaseUrl(space3dsUrlStr)
                        + DOCUMENT_SEARCH_ENDPOINT
                        + "?searchStr="
                        + encodedSearchStr;

        System.out.println(
                "Document search URL: "
                        + searchUrl);

        HttpResponse<String> searchResponse =
                httpRequestService.loadUrl(
                        "GET",
                        "",
                        "",
                        searchUrl,
                        headers);

        if (searchResponse.statusCode() != 200) {

            throw new RuntimeException(
                    "3DEXPERIENCE Document search failed. Status="
                            + searchResponse.statusCode()
                            + " Response="
                            + searchResponse.body());
        }

        List<DocumentInfo> documents =
                extractDocuments(
                        searchResponse.body());

        return new DocumentDto(
                true,
                searchResponse.statusCode(),
                effectiveSearchStr,
                documents);
    }

    public DocumentDetailDto getDocumentByPhysicalId(
            String physicalId) throws Exception {

        if (physicalId == null || physicalId.isBlank()) {

            throw new IllegalArgumentException(
                    "The document physicalId is required.");
        }

        prepareConnection();

        Map<String, String> headers =
                createSecurityHeaders();

        String effectivePhysicalId =
                physicalId.trim();

        String encodedPhysicalId =
                URLEncoder.encode(
                        effectivePhysicalId,
                        StandardCharsets.UTF_8);

        String documentUrl =
                normalizeBaseUrl(space3dsUrlStr)
                        + DOCUMENT_ENDPOINT
                        + encodedPhysicalId;

        System.out.println(
                "Document detail URL: "
                        + documentUrl);

        HttpResponse<String> documentResponse =
                httpRequestService.loadUrl(
                        "GET",
                        "",
                        "",
                        documentUrl,
                        headers);

        if (documentResponse.statusCode() != 200) {

            throw new RuntimeException(
                    "3DEXPERIENCE Document details failed. Status="
                            + documentResponse.statusCode()
                            + " PhysicalId="
                            + effectivePhysicalId
                            + " Response="
                            + documentResponse.body());
        }

        JSONParser parser =
                new JSONParser();

        JSONObject responseJson =
                (JSONObject) parser.parse(
                        documentResponse.body());

        JSONObject documentJson =
                extractFirstDocumentObject(
                        responseJson);

        if (documentJson == null) {

            throw new RuntimeException(
                    "No document found for physicalId "
                            + effectivePhysicalId);
        }

        DocumentInfo document =
                mapDocument(documentJson);

        return new DocumentDetailDto(
                true,
                documentResponse.statusCode(),
                effectivePhysicalId,
                document);
    }

    private void prepareConnection()
            throws Exception {

        service3DXConnexion
                .prepareAuthorizationHeaderValue();

        service3DXConnexion
                .getCsrfTokenValueBasic();
    }

    private List<DocumentInfo> extractDocuments(
            String responseBody) throws Exception {

        JSONParser parser =
                new JSONParser();

        JSONObject searchJson =
                (JSONObject) parser.parse(
                        responseBody);

        JSONArray documentArray =
                getJsonArray(
                        searchJson,
                        "data");

        List<DocumentInfo> result =
                new ArrayList<>();

        if (documentArray == null
                || documentArray.isEmpty()) {

            return result;
        }

        for (Object object : documentArray) {

            if (!(object instanceof JSONObject)) {
                continue;
            }

            JSONObject documentJson =
                    (JSONObject) object;

            DocumentInfo document =
                    mapDocument(documentJson);

            if (document != null) {
                result.add(document);
            }
        }

        return result;
    }

    private JSONObject extractFirstDocumentObject(
            JSONObject responseJson) {

        JSONArray data =
                getJsonArray(
                        responseJson,
                        "data");

        if (data == null || data.isEmpty()) {
            return null;
        }

        Object firstObject =
                data.get(0);

        if (firstObject instanceof JSONObject) {
            return (JSONObject) firstObject;
        }

        return null;
    }

    private DocumentInfo mapDocument(
            JSONObject documentJson) {

        if (documentJson == null) {
            return null;
        }

        JSONObject dataElements =
                getJsonObject(
                        documentJson,
                        "dataelements");

        JSONObject relatedData =
                getJsonObject(
                        documentJson,
                        "relateddata");

        JSONObject ownerDataElements =
                getFirstRelatedDataElements(
                        relatedData,
                        "ownerInfo");

        JSONObject originatorDataElements =
                getFirstRelatedDataElements(
                        relatedData,
                        "originatorInfo");

        DocumentInfo info =
                new DocumentInfo();

        info.setId(
                getString(documentJson, "id"));

        info.setType(
                getString(documentJson, "type"));

        info.setIdentifier(
                getString(documentJson, "identifier"));

        info.setRelativePath(
                getString(documentJson, "relativePath"));

        info.setName(
                getString(dataElements, "name"));

        info.setTitle(
                getString(dataElements, "title"));

        info.setRevision(
                getString(dataElements, "revision"));

        info.setState(
                getString(dataElements, "state"));

        info.setStateNLS(
                getString(dataElements, "stateNLS"));

        info.setPolicy(
                getString(dataElements, "policy"));

        info.setDescription(
                getString(dataElements, "description"));

        info.setCollabSpace(
                getString(dataElements, "collabspace"));

        info.setCollabSpaceTitle(
                getString(dataElements, "collabSpaceTitle"));

        info.setFileExtension(
                getString(dataElements, "fileExtension"));

        info.setOriginated(
                getString(dataElements, "originated"));

        info.setModified(
                getString(dataElements, "modified"));

        info.setIsLatestRevision(
                getString(dataElements, "isLatestRevision"));

        info.setHasDownloadAccess(
                getString(dataElements, "hasDownloadAccess"));

        info.setHasReviseAccess(
                getString(dataElements, "hasReviseAccess"));

        info.setHasModifyAccess(
                getString(dataElements, "hasModifyAccess"));

        info.setHasDeleteAccess(
                getString(dataElements, "hasDeleteAccess"));

        info.setReservedBy(
                getString(dataElements, "reservedby"));

        info.setCodification(
                getString(dataElements, "CODIFICATION"));

        info.setOwner(
                getString(ownerDataElements, "name"));

        info.setOwnerFirstName(
                getString(ownerDataElements, "firstname"));

        info.setOwnerLastName(
                getString(ownerDataElements, "lastname"));

        info.setOriginator(
                getString(originatorDataElements, "name"));

        info.setOriginatorFirstName(
                getString(originatorDataElements, "firstname"));

        info.setOriginatorLastName(
                getString(originatorDataElements, "lastname"));

        info.setFiles(
                extractDocumentFiles(
                        relatedData));

        return info;
    }

    private List<DocumentFileInfo> extractDocumentFiles(
            JSONObject relatedData) {

        List<DocumentFileInfo> result =
                new ArrayList<>();

        if (relatedData == null) {
            return result;
        }

        JSONArray files =
                getJsonArray(
                        relatedData,
                        "files");

        if (files == null || files.isEmpty()) {
            return result;
        }

        for (Object object : files) {

            if (!(object instanceof JSONObject)) {
                continue;
            }

            JSONObject fileJson =
                    (JSONObject) object;

            JSONObject fileDataElements =
                    getJsonObject(
                            fileJson,
                            "dataelements");

            JSONObject fileRelatedData =
                    getJsonObject(
                            fileJson,
                            "relateddata");

            JSONObject ownerDataElements =
                    getFirstRelatedDataElements(
                            fileRelatedData,
                            "ownerInfo");

            DocumentFileInfo fileInfo =
                    new DocumentFileInfo();

            fileInfo.setId(
                    getString(fileJson, "id"));

            fileInfo.setType(
                    getString(fileJson, "type"));

            fileInfo.setIdentifier(
                    getString(fileJson, "identifier"));

            fileInfo.setRelativePath(
                    getString(fileJson, "relativePath"));

            fileInfo.setRelationshipId(
                    getString(fileJson, "relId"));

            fileInfo.setTitle(
                    getString(fileDataElements, "title"));

            fileInfo.setName(
                    getString(fileDataElements, "name"));

            fileInfo.setRevision(
                    getString(fileDataElements, "revision"));

            fileInfo.setComments(
                    getString(fileDataElements, "comments"));

            fileInfo.setFileCategory(
                    getString(fileDataElements, "fileCategory"));

            fileInfo.setLocker(
                    getString(fileDataElements, "locker"));

            fileInfo.setFileType(
                    getString(fileDataElements, "fileType"));

            fileInfo.setDimension(
                    getString(fileDataElements, "dimension"));

            fileInfo.setLength(
                    getString(fileDataElements, "length"));

            fileInfo.setFileSize(
                    getString(fileDataElements, "fileSize"));

            fileInfo.setFileChecksum(
                    getString(fileDataElements, "fileChecksum"));

            fileInfo.setFormat(
                    getString(fileDataElements, "format"));

            fileInfo.setOriginated(
                    getString(fileDataElements, "originated"));

            fileInfo.setModified(
                    getString(fileDataElements, "modified"));

            fileInfo.setOwner(
                    getString(ownerDataElements, "name"));

            fileInfo.setOwnerFirstName(
                    getString(ownerDataElements, "firstname"));

            fileInfo.setOwnerLastName(
                    getString(ownerDataElements, "lastname"));

            result.add(fileInfo);
        }

        return result;
    }

    private JSONObject getFirstRelatedDataElements(
            JSONObject relatedData,
            String relationName) {

        if (relatedData == null) {
            return new JSONObject();
        }

        JSONArray relatedObjects =
                getJsonArray(
                        relatedData,
                        relationName);

        if (relatedObjects == null
                || relatedObjects.isEmpty()) {

            return new JSONObject();
        }

        Object firstObject =
                relatedObjects.get(0);

        if (!(firstObject instanceof JSONObject)) {
            return new JSONObject();
        }

        JSONObject relatedObject =
                (JSONObject) firstObject;

        return getJsonObject(
                relatedObject,
                "dataelements");
    }

    private Map<String, String> createSecurityHeaders() {

        Map<String, String> headers =
                new HashMap<>();

        headers.put(
                "SecurityContext",
                secContext);

        headers.put(
                "Accept",
                MediaType.APPLICATION_JSON_VALUE);

        return headers;
    }

    private JSONObject getJsonObject(
            JSONObject json,
            String key) {

        if (json == null) {
            return new JSONObject();
        }

        Object value =
                json.get(key);

        if (value instanceof JSONObject) {
            return (JSONObject) value;
        }

        return new JSONObject();
    }

    private JSONArray getJsonArray(
            JSONObject json,
            String key) {

        if (json == null) {
            return null;
        }

        Object value =
                json.get(key);

        if (value instanceof JSONArray) {
            return (JSONArray) value;
        }

        return null;
    }

    private String getString(
            JSONObject json,
            String key) {

        if (json == null) {
            return "";
        }

        Object value =
                json.get(key);

        return value == null
                ? ""
                : value.toString();
    }

    private String normalizeBaseUrl(
            String baseUrl) {

        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException(
                    "The property threedx.space-url is not configured.");
        }

        String normalizedUrl =
                baseUrl.trim();

        while (normalizedUrl.endsWith("/")) {

            normalizedUrl =
                    normalizedUrl.substring(
                            0,
                            normalizedUrl.length() - 1);
        }

        return normalizedUrl;
    }
}