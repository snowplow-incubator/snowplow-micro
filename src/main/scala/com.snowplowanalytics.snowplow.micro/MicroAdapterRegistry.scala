/*
 * Copyright (c) 2019-present Snowplow Analytics Ltd. All rights reserved.
 *
 * This software is made available by Snowplow Analytics, Ltd.,
 * under the terms of the Snowplow Limited Use License Agreement, Version 1.0
 * located at https://docs.snowplow.io/limited-use-license-1.0
 * BY INSTALLING, DOWNLOADING, ACCESSING, USING OR DISTRIBUTING ANY PORTION
 * OF THE SOFTWARE, YOU AGREE TO THE TERMS OF SUCH LICENSE AGREEMENT.
 */

package com.snowplowanalytics.snowplow.micro

import cats.effect.IO
import com.snowplowanalytics.snowplow.enrich.common.adapters._

object MicroAdapterRegistry {
 
  private val adaptersSchemas = AdaptersSchemas(
    CallrailSchemas("iglu:com.callrail/call_complete/jsonschema/1-0-2"),
    CloudfrontAccessLogSchemas(
      "iglu:com.amazon.aws.cloudfront/wd_access_log/jsonschema/1-0-2",
      "iglu:com.amazon.aws.cloudfront/wd_access_log/jsonschema/1-0-3",
      "iglu:com.amazon.aws.cloudfront/wd_access_log/jsonschema/1-0-1",
      "iglu:com.amazon.aws.cloudfront/wd_access_log/jsonschema/1-0-0",
      "iglu:com.amazon.aws.cloudfront/wd_access_log/jsonschema/1-0-4",
      "iglu:com.amazon.aws.cloudfront/wd_access_log/jsonschema/1-0-5",
      "iglu:com.amazon.aws.cloudfront/wd_access_log/jsonschema/1-0-6"
    ),
    GoogleAnalyticsSchemas(
      "iglu:com.google.analytics.measurement-protocol/page_view/jsonschema/1-0-0",
      "iglu:com.google.analytics.measurement-protocol/screen_view/jsonschema/1-0-0",
      "iglu:com.google.analytics.measurement-protocol/event/jsonschema/1-0-0",
      "iglu:com.google.analytics.measurement-protocol/transaction/jsonschema/1-0-0",
      "iglu:com.google.analytics.measurement-protocol/item/jsonschema/1-0-0",
      "iglu:com.google.analytics.measurement-protocol/social/jsonschema/1-0-0",
      "iglu:com.google.analytics.measurement-protocol/exception/jsonschema/1-0-0",
      "iglu:com.google.analytics.measurement-protocol/timing/jsonschema/1-0-0",
      "iglu:com.google.analytics/undocumented/jsonschema/1-0-0",
      "iglu:com.google.analytics/private/jsonschema/1-0-0",
      "iglu:com.google.analytics.measurement-protocol/general/jsonschema/1-0-0",
      "iglu:com.google.analytics.measurement-protocol/user/jsonschema/1-0-0",
      "iglu:com.google.analytics.measurement-protocol/session/jsonschema/1-0-0",
      "iglu:com.google.analytics.measurement-protocol/traffic_source/jsonschema/1-0-0",
      "iglu:com.google.analytics.measurement-protocol/system_info/jsonschema/1-0-0",
      "iglu:com.google.analytics.measurement-protocol/link/jsonschema/1-0-0",
      "iglu:com.google.analytics.measurement-protocol/app/jsonschema/1-0-0",
      "iglu:com.google.analytics.measurement-protocol/product_action/jsonschema/1-0-0",
      "iglu:com.google.analytics.measurement-protocol/content_experiment/jsonschema/1-0-0",
      "iglu:com.google.analytics.measurement-protocol/hit/jsonschema/1-0-0",
      "iglu:com.google.analytics.measurement-protocol/promotion_action/jsonschema/1-0-0",
      "iglu:com.google.analytics.measurement-protocol/product/jsonschema/1-0-0",
      "iglu:com.google.analytics.measurement-protocol/product_custom_dimension/jsonschema/1-0-0",
      "iglu:com.google.analytics.measurement-protocol/product_custom_metric/jsonschema/1-0-0",
      "iglu:com.google.analytics.measurement-protocol/product_impression_list/jsonschema/1-0-0",
      "iglu:com.google.analytics.measurement-protocol/product_impression/jsonschema/1-0-0",
      "iglu:com.google.analytics.measurement-protocol/product_impression_custom_dimension/jsonschema/1-0-0",
      "iglu:com.google.analytics.measurement-protocol/product_impression_custom_metric/jsonschema/1-0-0",
      "iglu:com.google.analytics.measurement-protocol/promotion/jsonschema/1-0-0",
      "iglu:com.google.analytics.measurement-protocol/custom_dimension/jsonschema/1-0-0",
      "iglu:com.google.analytics.measurement-protocol/custom_metric/jsonschema/1-0-0",
      "iglu:com.google.analytics.measurement-protocol/content_group/jsonschema/1-0-0"
    ),
    HubspotSchemas(
      "iglu:com.hubspot/contact_creation/jsonschema/1-0-0",
      "iglu:com.hubspot/contact_deletion/jsonschema/1-0-0",
      "iglu:com.hubspot/contact_change/jsonschema/1-0-0",
      "iglu:com.hubspot/company_creation/jsonschema/1-0-0",
      "iglu:com.hubspot/company_deletion/jsonschema/1-0-0",
      "iglu:com.hubspot/company_change/jsonschema/1-0-0",
      "iglu:com.hubspot/deal_creation/jsonschema/1-0-0",
      "iglu:com.hubspot/deal_deletion/jsonschema/1-0-0",
      "iglu:com.hubspot/deal_change/jsonschema/1-0-0"
    ),
    MailchimpSchemas(
      "iglu:com.mailchimp/subscribe/jsonschema/1-0-0",
      "iglu:com.mailchimp/unsubscribe/jsonschema/1-0-0",
      "iglu:com.mailchimp/campaign_sending_status/jsonschema/1-0-0",
      "iglu:com.mailchimp/cleaned_email/jsonschema/1-0-0",
      "iglu:com.mailchimp/email_address_change/jsonschema/1-0-0",
      "iglu:com.mailchimp/profile_update/jsonschema/1-0-0"
    ),
    MailgunSchemas(
      "iglu:com.mailgun/message_bounced/jsonschema/1-0-0",
      "iglu:com.mailgun/message_clicked/jsonschema/1-0-0",
      "iglu:com.mailgun/message_complained/jsonschema/1-0-0",
      "iglu:com.mailgun/message_delivered/jsonschema/1-0-0",
      "iglu:com.mailgun/message_dropped/jsonschema/1-0-0",
      "iglu:com.mailgun/message_opened/jsonschema/1-0-0",
      "iglu:com.mailgun/recipient_unsubscribed/jsonschema/1-0-0"
    ),
    MandrillSchemas(
      "iglu:com.mandrill/message_bounced/jsonschema/1-0-1",
      "iglu:com.mandrill/message_clicked/jsonschema/1-0-1",
      "iglu:com.mandrill/message_delayed/jsonschema/1-0-1",
      "iglu:com.mandrill/message_marked_as_spam/jsonschema/1-0-1",
      "iglu:com.mandrill/message_opened/jsonschema/1-0-1",
      "iglu:com.mandrill/message_rejected/jsonschema/1-0-0",
      "iglu:com.mandrill/message_sent/jsonschema/1-0-0",
      "iglu:com.mandrill/message_soft_bounced/jsonschema/1-0-1",
      "iglu:com.mandrill/recipient_unsubscribed/jsonschema/1-0-1"
    ),
    MarketoSchemas("iglu:com.marketo/event/jsonschema/2-0-0"),
    OlarkSchemas(
      "iglu:com.olark/transcript/jsonschema/1-0-0",
      "iglu:com.olark/offline_message/jsonschema/1-0-0"
    ),
    PagerdutySchemas(
      "iglu:com.pagerduty/incident/jsonschema/1-0-0"
    ),
    PingdomSchemas(
      "iglu:com.pingdom/incident_assign/jsonschema/1-0-0",
      "iglu:com.pingdom/incident_notify_user/jsonschema/1-0-0",
      "iglu:com.pingdom/incident_notify_of_close/jsonschema/1-0-0"
    ),
    SendgridSchemas(
      "iglu:com.sendgrid/processed/jsonschema/3-0-0",
      "iglu:com.sendgrid/dropped/jsonschema/3-0-0",
      "iglu:com.sendgrid/delivered/jsonschema/3-0-0",
      "iglu:com.sendgrid/deferred/jsonschema/3-0-0",
      "iglu:com.sendgrid/bounce/jsonschema/3-0-0",
      "iglu:com.sendgrid/open/jsonschema/3-0-0",
      "iglu:com.sendgrid/click/jsonschema/3-0-0",
      "iglu:com.sendgrid/spamreport/jsonschema/3-0-0",
      "iglu:com.sendgrid/unsubscribe/jsonschema/3-0-0",
      "iglu:com.sendgrid/group_unsubscribe/jsonschema/3-0-0",
      "iglu:com.sendgrid/group_resubscribe/jsonschema/3-0-0"
    ),
    StatusGatorSchemas(
      "iglu:com.statusgator/status_change/jsonschema/1-0-0"
    ),
    UnbounceSchemas(
      "iglu:com.unbounce/form_post/jsonschema/1-0-0"
    ),
    UrbanAirshipSchemas(
      "iglu:com.urbanairship.connect/CLOSE/jsonschema/1-0-0",
      "iglu:com.urbanairship.connect/CUSTOM/jsonschema/1-0-0",
      "iglu:com.urbanairship.connect/FIRST_OPEN/jsonschema/1-0-0",
      "iglu:com.urbanairship.connect/IN_APP_MESSAGE_DISPLAY/jsonschema/1-0-0",
      "iglu:com.urbanairship.connect/IN_APP_MESSAGE_EXPIRATION/jsonschema/1-0-0",
      "iglu:com.urbanairship.connect/IN_APP_MESSAGE_RESOLUTION/jsonschema/1-0-0",
      "iglu:com.urbanairship.connect/LOCATION/jsonschema/1-0-0",
      "iglu:com.urbanairship.connect/OPEN/jsonschema/1-0-0",
      "iglu:com.urbanairship.connect/PUSH_BODY/jsonschema/1-0-0",
      "iglu:com.urbanairship.connect/REGION/jsonschema/1-0-0",
      "iglu:com.urbanairship.connect/RICH_DELETE/jsonschema/1-0-0",
      "iglu:com.urbanairship.connect/RICH_DELIVERY/jsonschema/1-0-0",
      "iglu:com.urbanairship.connect/RICH_HEAD/jsonschema/1-0-0",
      "iglu:com.urbanairship.connect/SEND/jsonschema/1-0-0",
      "iglu:com.urbanairship.connect/TAG_CHANGE/jsonschema/1-0-0",
      "iglu:com.urbanairship.connect/UNINSTALL/jsonschema/1-0-0"
    ),
    VeroSchemas(
      "iglu:com.getvero/bounced/jsonschema/1-0-0",
      "iglu:com.getvero/clicked/jsonschema/1-0-0",
      "iglu:com.getvero/delivered/jsonschema/1-0-0",
      "iglu:com.getvero/opened/jsonschema/1-0-0",
      "iglu:com.getvero/sent/jsonschema/1-0-0",
      "iglu:com.getvero/unsubscribed/jsonschema/1-0-0",
      "iglu:com.getvero/created/jsonschema/1-0-0",
      "iglu:com.getvero/updated/jsonschema/1-0-0"
    )
  )

  def create(): AdapterRegistry[IO] = {
    new AdapterRegistry[IO](Map.empty, adaptersSchemas)
  }
}
