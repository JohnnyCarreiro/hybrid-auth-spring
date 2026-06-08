package com.johnnycarreiro.hybridauth.auth.web.jwks;

import com.johnnycarreiro.hybridauth.auth.domain.signing.SigningKeys;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The published JWKS endpoint (SDD-001 §7): {@code GET /.well-known/jwks.json}.
 *
 * <p>This is the <em>only</em> seam out of the auth-service (AGENTS.md / SDD-001 §1): the
 * resource-service fetches it to verify access tokens locally, with no shared secret and no
 * per-request call back. The body is the <strong>public-only</strong> set of servable keys (active
 * + any key inside its grace window) — {@link SigningKeys#publicJwkSet()} strips every private
 * parameter, so signing material can never leak here.
 *
 * <p>{@code Cache-Control: public, max-age=600} lets the resource-service (and any intermediary)
 * cache the set for ten minutes; rotation's grace window is far longer, so a cached set stays able
 * to verify freshly minted tokens.
 */
@RestController
public class JwksController {

  private final SigningKeys keys;

  public JwksController(SigningKeys keys) {
    this.keys = keys;
  }

  @GetMapping("/.well-known/jwks.json")
  public ResponseEntity<Map<String, Object>> jwks() {
    Map<String, Object> body = keys.publicJwkSet().toJSONObject();
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(java.time.Duration.ofSeconds(600)).cachePublic())
        .body(body);
  }
}
