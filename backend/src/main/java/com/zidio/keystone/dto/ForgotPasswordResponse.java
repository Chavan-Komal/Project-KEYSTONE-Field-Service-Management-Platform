package com.zidio.keystone.dto;

/**
 * Always returns the same generic {@code message} regardless of whether the
 * email matched an account. {@code resetUrl} is populated only when
 * {@code keystone.auth.expose-reset-token=true} (handy for local dev and
 * demos where there's no mail server) — set it to false in production so the
 * link only travels by email / server log.
 */
public record ForgotPasswordResponse(
    String message,
    String resetUrl
) {}
