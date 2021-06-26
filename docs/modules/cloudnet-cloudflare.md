### Cloudflare

The Cloudflare Module allows automatic entry of Domain DNSRecords for larger required network capacities. These can be
made from any type of application, from bungee cord to normal vanilla servers, the group sets the tone. For the
configuration you need a domain and an account at the provider https://cloudflare.com. Several domains with different
users etc. can be managed, even for the same groups. You need the email, the API key from the account (not the global
API key!), The ZoneId found on the domain's dashboard page.

The "@" wildcard in the "sub" configuration of a group determines that it does not become a subdomain that owns a
third-level domain, but only a second-level domain. The configuration must be set up in the cluster at each node
individually, so that one can avoid unintentional entries such as nodes that are within an internal LAN and can only be
reached via specific ports or proxy servers.
