package org.openexi.scomp;

final class Production extends Substance {
  
  private final Event m_event;
  private final ProtoGrammar m_subsequentGrammar;
  
  private final int m_particleNumber;
  
  public Production(Event event, ProtoGrammar protoGrammar) {
    this(event, protoGrammar, -1);
  }
  
  public Production(Event event, ProtoGrammar subsequentGrammar, int particleNumber) {
    m_event = event;
    m_subsequentGrammar = subsequentGrammar;
    m_particleNumber = particleNumber;
  }
  
  @Override
  public final boolean isProduction() {
    return true;
  }

  @Override
  public final RHSType getRHSType() {
    return RHSType.PROD;
  }
  
  public Event getEvent() {
    return m_event;
  }
  
  public ProtoGrammar getSubsequentGrammar() {
    return m_subsequentGrammar;
  }

  public int getParticleNumber() {
    return m_particleNumber;
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Production) {
      final Production production = (Production)obj;
      return m_event.equals(production.m_event) && m_subsequentGrammar == production.m_subsequentGrammar;
    }
    return false;
  }

  @Override
  final short getPriority() {
    return m_event.getEventType();
  }

  static int compareAT(Production production1, Production production2) {
    final EventAT at1 = (EventAT)production1.m_event;
    final EventAT at2 = (EventAT)production2.m_event;
    int res = 0;
    if (production1 != production2) {
      if ((res = at1.getLocalName().compareTo(at2.getLocalName())) == 0)
        res = at1.getUri().compareTo(at2.getUri());
      assert res != 0;
    }
    return res;
  }

  static int compareATWildcardNS(Production production1, Production production2) {
    EventATWildcardNS at1 = (EventATWildcardNS)production1.m_event;
    EventATWildcardNS at2 = (EventATWildcardNS)production2.m_event;
    int res = at1.getUri().compareTo(at2.getUri());
    assert res != 0;
    return res;
  }

  static int compareSE(Production production1, Production production2) {
    final int res = production1.m_particleNumber - production2.m_particleNumber;
    return res;
  }

}
