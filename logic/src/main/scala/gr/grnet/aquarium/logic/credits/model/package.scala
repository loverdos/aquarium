package gr.grnet.aquarium.logic.credits

package object model {
  /**
   * For a member of a structure, provide the credit distribution type
   * the parent follows for this member.
   */
  type MembersCreditDistributionMap = Map[CreditHolder, List[CreditDistributionPolicy]]
}