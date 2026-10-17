package academy.tochkavhoda.competition.model;

import java.util.Objects;

/**
 * Holds information about an active user session identified by a token.
 */
public class AuthToken {
    public enum Role {
        CONTENDER, REVIEWER
    }

    private String tokenValue;
    private int linkedUserId;
    private Role role;

    public AuthToken() {
    }

    public AuthToken(String tokenValue, int linkedUserId, Role role) {
        this.tokenValue = tokenValue;
        this.linkedUserId = linkedUserId;
        this.role = role;
    }

    public String getTokenValue() {
        return tokenValue;
    }

    public void setTokenValue(String tokenValue) {
        this.tokenValue = tokenValue;
    }

    public int getLinkedUserId() {
        return linkedUserId;
    }

    public void setLinkedUserId(int linkedUserId) {
        this.linkedUserId = linkedUserId;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        AuthToken authToken = (AuthToken) other;
        return Objects.equals(tokenValue, authToken.tokenValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tokenValue);
    }
}
