package academy.tochkavhoda.competition.service;

import academy.tochkavhoda.competition.dao.Storage;
import academy.tochkavhoda.competition.dto.request.AddApplicationDtoRequest;
import academy.tochkavhoda.competition.dto.request.LoginDtoRequest;
import academy.tochkavhoda.competition.dto.request.RegisterParticipantDtoRequest;
import academy.tochkavhoda.competition.dto.response.LoginDtoResponse;
import academy.tochkavhoda.competition.model.AuthToken;
import academy.tochkavhoda.competition.model.Contender;
import academy.tochkavhoda.competition.model.GrantRequest;
import academy.tochkavhoda.competition.server.ServerResponse;
import com.google.gson.Gson;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Business logic for contenders (firms submitting grant requests).
 */
public class ContenderService {
    private final Storage storage;
    private final Gson gson = new Gson();

    public ContenderService(Storage storage) {
        this.storage = storage;
    }

    public ServerResponse registerContender(String jsonPayload) {
        try {
            RegisterParticipantDtoRequest dto = gson.fromJson(jsonPayload, RegisterParticipantDtoRequest.class);
            checkRegistrationPayload(dto);

            String loginKey = dto.getLogin().trim();
            if (storage.lookupContenderByLogin(loginKey).isPresent()
                    || storage.lookupReviewerByLogin(loginKey).isPresent()) {
                return ServerResponse.error("Этот логин уже используется");
            }

            Contender entity = new Contender(
                    0,
                    dto.getFirstName().trim(),
                    dto.getLastName().trim(),
                    dto.getCompanyName().trim(),
                    loginKey,
                    dto.getPassword()
            );
            storage.persistContender(entity);
            return ServerResponse.success();
        } catch (IllegalArgumentException ex) {
            return ServerResponse.error(ex.getMessage());
        } catch (Exception ex) {
            return ServerResponse.error("Некорректный формат запроса");
        }
    }

    public ServerResponse authenticate(String jsonPayload) {
        try {
            LoginDtoRequest dto = gson.fromJson(jsonPayload, LoginDtoRequest.class);
            InputChecker.checkLoginFormat(dto.getLogin());
            InputChecker.requireNonBlank(dto.getPassword(), "Пароль");

            Contender found = storage.lookupContenderByLogin(dto.getLogin().trim()).orElse(null);
            if (found == null || !found.getCredentialsSecret().equals(dto.getPassword())) {
                return ServerResponse.error("Неверный логин или пароль");
            }

            String tokenStr = UUID.randomUUID().toString();
            AuthToken session = new AuthToken(tokenStr, found.getIdentifier(), AuthToken.Role.CONTENDER);
            storage.storeToken(session);

            return ServerResponse.success(gson.toJson(new LoginDtoResponse(tokenStr)));
        } catch (IllegalArgumentException ex) {
            return ServerResponse.error(ex.getMessage());
        } catch (Exception ex) {
            return ServerResponse.error("Некорректный формат запроса");
        }
    }

    public ServerResponse signOut(String tokenStr) {
        try {
            resolveContenderSession(tokenStr);
            storage.invalidateToken(tokenStr);
            return ServerResponse.success();
        } catch (IllegalArgumentException ex) {
            return ServerResponse.error(ex.getMessage());
        }
    }

    public ServerResponse withdrawCompletely(String tokenStr) {
        try {
            AuthToken session = resolveContenderSession(tokenStr);
            storage.eraseContender(session.getLinkedUserId());
            return ServerResponse.success();
        } catch (IllegalArgumentException ex) {
            return ServerResponse.error(ex.getMessage());
        }
    }

    public ServerResponse submitRequest(String tokenStr, String jsonPayload) {
        try {
            if (storage.isContestFinished()) {
                return ServerResponse.error("Конкурс закрыт, подача заявок невозможна");
            }
            AuthToken session = resolveContenderSession(tokenStr);
            AddApplicationDtoRequest dto = gson.fromJson(jsonPayload, AddApplicationDtoRequest.class);
            checkRequestPayload(dto);

            GrantRequest entity = new GrantRequest(
                    0,
                    session.getLinkedUserId(),
                    dto.getTitle().trim(),
                    dto.getDescription().trim(),
                    dto.getDirections(),
                    dto.getAmount()
            );
            storage.persistGrantRequest(entity);
            return ServerResponse.success("{\"id\":" + entity.getIdentifier() + "}");
        } catch (IllegalArgumentException ex) {
            return ServerResponse.error(ex.getMessage());
        } catch (Exception ex) {
            return ServerResponse.error("Некорректный формат запроса");
        }
    }

    public ServerResponse revokeRequest(String tokenStr, String jsonPayload) {
        try {
            AuthToken session = resolveContenderSession(tokenStr);
            int requestId = gson.fromJson(jsonPayload, com.google.gson.JsonObject.class).get("id").getAsInt();

            GrantRequest req = storage.lookupGrantRequestById(requestId)
                    .orElseThrow(() -> new IllegalArgumentException("Заявка не найдена"));
            if (req.getOwnerContenderId() != session.getLinkedUserId()) {
                return ServerResponse.error("Можно отменить только свою заявку");
            }
            storage.eraseGrantRequest(requestId);
            return ServerResponse.success();
        } catch (IllegalArgumentException ex) {
            return ServerResponse.error(ex.getMessage());
        } catch (Exception ex) {
            return ServerResponse.error("Некорректный формат запроса");
        }
    }

    public ServerResponse listOwnRequests(String tokenStr) {
        try {
            AuthToken session = resolveContenderSession(tokenStr);
            List<GrantRequest> owned = storage.findRequestsOwnedBy(session.getLinkedUserId());
            String json = gson.toJson(owned.stream().map(this::toSummary).collect(Collectors.toList()));
            return ServerResponse.success(json);
        } catch (IllegalArgumentException ex) {
            return ServerResponse.error(ex.getMessage());
        }
    }

    private AuthToken resolveContenderSession(String tokenStr) {
        AuthToken session = storage.lookupToken(tokenStr)
                .orElseThrow(() -> new IllegalArgumentException("Недействительный токен"));
        if (session.getRole() != AuthToken.Role.CONTENDER) {
            throw new IllegalArgumentException("Токен не принадлежит участнику");
        }
        return session;
    }

    private void checkRegistrationPayload(RegisterParticipantDtoRequest dto) {
        InputChecker.requireMinLength(dto.getFirstName(), InputChecker.NAME_MIN_CHARS, "Имя");
        InputChecker.requireMinLength(dto.getLastName(), InputChecker.NAME_MIN_CHARS, "Фамилия");
        InputChecker.requireMinLength(dto.getCompanyName(), InputChecker.ORG_MIN_CHARS, "Название фирмы");
        InputChecker.checkLoginFormat(dto.getLogin());
        InputChecker.checkSecretStrength(dto.getPassword());
    }

    private void checkRequestPayload(AddApplicationDtoRequest dto) {
        InputChecker.requireMinLength(dto.getTitle(), InputChecker.TITLE_MIN_CHARS, "Название заявки");
        InputChecker.requireMinLength(dto.getDescription(), InputChecker.DESC_MIN_CHARS, "Описание заявки");
        if (dto.getDirections() == null || dto.getDirections().isEmpty()) {
            throw new IllegalArgumentException("Список направлений не может быть пустым");
        }
        InputChecker.checkPositiveSum(dto.getAmount());
    }

    private java.util.Map<String, Object> toSummary(GrantRequest req) {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id", req.getIdentifier());
        map.put("title", req.getRequestTitle());
        map.put("description", req.getRequestDescription());
        map.put("directions", req.getSubjectAreas());
        map.put("amount", req.getRequestedSum());
        map.put("averageScore", req.computeAverageScore());
        return map;
    }
}
