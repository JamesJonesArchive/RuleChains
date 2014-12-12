RuleChains
==========

RuleChains provides a mechanism to wrap SQL and Services as business rules for aggregate processing without having
to construct scripts (feeds).

==========
What is RuleChains? RuleChains provides a mechanism to wrap SQL and Services as business rules for aggregate processing without having to construct custom scripts (feeds). RuleChains is somewhat analogous to a pattern-based sequencer (a musician reference) for business rules. "Rules" are assembled into "Link" patterns and formed into "Chains" that can be triggered by a scheduling event or as an incoming REST event. A "chain" is actually just a sequence of "links". Each link contains a "rule" as well as it's execution properties and custom input/output handling (if needed).

1. [Overview](../../wiki/Overview)
2. [Configuration](../../wiki/Configuration)
3. [How a "Link" functions](../../wiki/How-a-"Link"-functions)
4. [Sample 1: Build a table and putting data in it](../../wiki/Sample-1:-Build-a-table-and-putting-data-in-it)
5. [Groovy Rule Magic](../../wiki/Groovy-Rule-Magic)
