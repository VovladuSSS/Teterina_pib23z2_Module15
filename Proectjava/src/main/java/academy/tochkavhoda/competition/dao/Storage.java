package academy.tochkavhoda.competition.dao;

import academy.tochkavhoda.competition.model.AuthToken;
import academy.tochkavhoda.competition.model.Contender;
import academy.tochkavhoda.competition.model.GrantRequest;
import academy.tochkavhoda.competition.model.Reviewer;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Abstraction over in-memory data storage for the grant competition.
 */
public interface Storage {

    // Contenders
    Contender persistContender(Contender contender);
    Optional<Contender> lookupContenderById(int id);
    Optional<Contender> lookupContenderByLogin(String login);
    Collection<Contender> fetchAllContenders();
    void eraseContender(int id);

    // Reviewers
    Reviewer persistReviewer(Reviewer reviewer);
    Optional<Reviewer> lookupReviewerById(int id);
    Optional<Reviewer> lookupReviewerByLogin(String login);
    Collection<Reviewer> fetchAllReviewers();
    void eraseReviewer(int id);

    // Grant requests
    GrantRequest persistGrantRequest(GrantRequest request);
    Optional<GrantRequest> lookupGrantRequestById(int id);
    Collection<GrantRequest> fetchAllGrantRequests();
    void eraseGrantRequest(int id);
    List<GrantRequest> findRequestsMatchingAreas(Collection<String> areas);
    List<GrantRequest> findRequestsOwnedBy(int contenderId);

    // Sessions / tokens
    void storeToken(AuthToken token);
    Optional<AuthToken> lookupToken(String tokenValue);
    void invalidateToken(String tokenValue);
    void invalidateTokensForUser(int userId, AuthToken.Role role);

    // Competition parameters
    void updateTotalFund(long amount);
    long readTotalFund();
    void updateScoreThreshold(double threshold);
    double readScoreThreshold();
    boolean isContestFinished();
    void markContestFinished();
    void resetContestState();
    List<GrantRequest> readWinningRequests();
    void writeWinningRequests(List<GrantRequest> winners);

    // Id generators
    int allocateContenderId();
    int allocateReviewerId();
    int allocateRequestId();
}
