package academy.tochkavhoda.competition.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a firm / organization taking part in the grant contest.
 */
public class Contender {
    private int identifier;
    private String givenName;
    private String familyName;
    private String organizationTitle;
    private String credentialsLogin;
    private String credentialsSecret;
    private List<Integer> submittedRequestIds = new ArrayList<>();

    public Contender() {
    }

    public Contender(int identifier, String givenName, String familyName, String organizationTitle,
                     String credentialsLogin, String credentialsSecret) {
        this.identifier = identifier;
        this.givenName = givenName;
        this.familyName = familyName;
        this.organizationTitle = organizationTitle;
        this.credentialsLogin = credentialsLogin;
        this.credentialsSecret = credentialsSecret;
    }

    public int getIdentifier() {
        return identifier;
    }

    public void setIdentifier(int identifier) {
        this.identifier = identifier;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getOrganizationTitle() {
        return organizationTitle;
    }

    public void setOrganizationTitle(String organizationTitle) {
        this.organizationTitle = organizationTitle;
    }

    public String getCredentialsLogin() {
        return credentialsLogin;
    }

    public void setCredentialsLogin(String credentialsLogin) {
        this.credentialsLogin = credentialsLogin;
    }

    public String getCredentialsSecret() {
        return credentialsSecret;
    }

    public void setCredentialsSecret(String credentialsSecret) {
        this.credentialsSecret = credentialsSecret;
    }

    public List<Integer> getSubmittedRequestIds() {
        return submittedRequestIds;
    }

    public void setSubmittedRequestIds(List<Integer> submittedRequestIds) {
        this.submittedRequestIds = submittedRequestIds;
    }

    public void attachRequestId(int requestId) {
        this.submittedRequestIds.add(requestId);
    }

    public void detachRequestId(int requestId) {
        this.submittedRequestIds.remove(Integer.valueOf(requestId));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        Contender contender = (Contender) other;
        return identifier == contender.identifier;
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }
}
