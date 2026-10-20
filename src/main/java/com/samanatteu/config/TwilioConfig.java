package com.samanatteu.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.twilio.http.TwilioRestClient;

/**
 * Le client Twilio, construit à partir des identifiants du compte.
 *
 * <p>Les identifiants ne sont jamais dans le dépôt : ils viennent du {@code .env}
 * (voir {@code .env.example}), lus par {@code application.yaml}.
 *
 * <p>On construit une <b>instance</b> de {@code TwilioRestClient} plutôt que d'appeler
 * {@code Twilio.init(...)} : pas d'état global, et le client est injectable — donc
 * testable.
 */
@Configuration
@ConditionalOnProperty(name = "samanatteu.sms.twilio.enabled", havingValue = "true")
public class TwilioConfig {

    @Bean
    public TwilioRestClient twilioRestClient(
            @Value("${samanatteu.sms.twilio.account-sid}") String accountSid,
            @Value("${samanatteu.sms.twilio.auth-token}") String authToken) {
        // Le Builder porte les identifiants : il signe chaque appel (Basic auth).
        return new TwilioRestClient.Builder(accountSid, authToken).build();
    }
}
