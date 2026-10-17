package academy.tochkavhoda.competition.server;

import academy.tochkavhoda.competition.dao.Storage;
import academy.tochkavhoda.competition.daoimpl.InMemoryStorage;
import academy.tochkavhoda.competition.service.ContenderService;
import academy.tochkavhoda.competition.service.GrantService;
import academy.tochkavhoda.competition.service.ReviewerService;

/**
 * Entry point of the grant competition server.
 * All persistent state lives in memory for the lifetime of the process.
 * Public methods follow the protocol described in the technical specification.
 */
public class Server {
    private final ContenderService contenderService;
    private final ReviewerService reviewerService;
    private final GrantService grantService;

    public Server() {
        Storage storage = new InMemoryStorage();
        this.contenderService = new ContenderService(storage);
        this.reviewerService = new ReviewerService(storage);
        this.grantService = new GrantService(storage);
    }

    // ---------- Contender (participant) operations ----------

    public ServerResponse registerParticipant(String requestJsonString) {
        return contenderService.registerContender(requestJsonString);
    }

    /**
     * Alias kept for compatibility with the example method name in the TZ.
     */
    public ServerResponse addParticipant(String token, String requestJsonString) {
        return registerParticipant(requestJsonString);
    }

    public ServerResponse loginParticipant(String requestJsonString) {
        return contenderService.authenticate(requestJsonString);
    }

    public ServerResponse logoutParticipant(String token) {
        return contenderService.signOut(token);
    }

    public ServerResponse leaveParticipant(String token) {
        return contenderService.withdrawCompletely(token);
    }

    public ServerResponse addApplication(String token, String requestJsonString) {
        return contenderService.submitRequest(token, requestJsonString);
    }

    public ServerResponse cancelApplication(String token, String requestJsonString) {
        return contenderService.revokeRequest(token, requestJsonString);
    }

    public ServerResponse getMyApplications(String token) {
        return contenderService.listOwnRequests(token);
    }

    // ---------- Reviewer (expert) operations ----------

    public ServerResponse registerExpert(String requestJsonString) {
        return reviewerService.registerReviewer(requestJsonString);
    }

    public ServerResponse loginExpert(String requestJsonString) {
        return reviewerService.authenticate(requestJsonString);
    }

    public ServerResponse logoutExpert(String token) {
        return reviewerService.signOut(token);
    }

    public ServerResponse leaveExpert(String token) {
        return reviewerService.withdrawCompletely(token);
    }

    public ServerResponse rateApplication(String token, String requestJsonString) {
        return reviewerService.assignScore(token, requestJsonString);
    }

    public ServerResponse deleteRating(String token, String requestJsonString) {
        return reviewerService.removeScore(token, requestJsonString);
    }

    public ServerResponse getApplicationsByDirections(String token, String requestJsonString) {
        return reviewerService.listByAreas(token, requestJsonString);
    }

    public ServerResponse getRatedApplications(String token) {
        return reviewerService.listScoredByMe(token);
    }

    // ---------- Shared / contest-wide operations ----------

    /**
     * Unified login that first tries contender credentials, then reviewer.
     */
    public ServerResponse login(String requestJsonString) {
        ServerResponse attempt = contenderService.authenticate(requestJsonString);
        if (attempt.getResponseCode() == 200) {
            return attempt;
        }
        return reviewerService.authenticate(requestJsonString);
    }

    public ServerResponse getParticipants(String token, String requestJsonString) {
        return grantService.listAllContenders(token, requestJsonString);
    }

    public ServerResponse getAllApplications(String token, String requestJsonString) {
        return grantService.listAllRequests(token, requestJsonString);
    }

    public ServerResponse setTotalFund(String token, String requestJsonString) {
        return grantService.configureFundSize(token, requestJsonString);
    }

    public ServerResponse setThreshold(String token, String requestJsonString) {
        return grantService.configureScoreCutoff(token, requestJsonString);
    }

    public ServerResponse closeAndCalculateResults(String token) {
        return grantService.finalizeRanking(token);
    }

    public ServerResponse getResults(String token) {
        return grantService.fetchFinalResults(token);
    }
}
