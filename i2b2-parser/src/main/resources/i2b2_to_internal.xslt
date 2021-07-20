<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output method="xml"/>
    <xsl:template match="query_definition">
        <query>
            <xsl:for-each select="panel">
                <inclusionCriteria>
                    <xsl:for-each select="item">
                        <criterion>
                            <termCode>
                                <code>
                                    <xsl:value-of select="item_key"/>
                                </code>
                                <system>i2b2_sim</system>
                                <display>
                                    <xsl:value-of select="item_name"/>
                                </display>
                            </termCode>
                            <xsl:if test="count(constrain_by_value) &gt; 0">
                                <valueFilter>
                                    <!-- Constraint by value -->
                                    <comparator>
                                        <xsl:value-of select="constrain_by_value/value_operator"/>
                                    </comparator>
                                    <unit>
                                        <xsl:value-of select="constrain_by_value/value_unit_of_measure"/>
                                    </unit>

                                    <xsl:choose>
                                        <!-- if a between operator is used, the boundaries are merged into a single tag -->
                                        <xsl:when test="constrain_by_value/value_operator/text()='between'">
                                            <filter>QUANTITY_RANGE</filter>

                                            <xsl:param name="values_text"
                                                       select="constrain_by_value/value_constraint/text()"/>
                                            <xsl:param name="first_value"
                                                       select="substring-before($values_text,' and ')"/>
                                            <xsl:param name="second_value"
                                                       select="substring-after($values_text, ' and ')"/>
                                            <!-- Properly order both values by size -->
                                            <xsl:choose>
                                                <xsl:when test="$first_value&gt;=$second_value">
                                                    <minValue>
                                                        <xsl:value-of select="$second_value"/>
                                                    </minValue>
                                                    <maxValue>
                                                        <xsl:value-of select="$first_value"/>
                                                    </maxValue>
                                                </xsl:when>
                                                <xsl:otherwise>
                                                    <minValue>
                                                        <xsl:value-of select="$first_value"/>
                                                    </minValue>
                                                    <maxValue>
                                                        <xsl:value-of select="$second_value"/>
                                                    </maxValue>
                                                </xsl:otherwise>
                                            </xsl:choose>

                                        </xsl:when>

                                        <!-- all other binary value operators -->
                                        <xsl:otherwise>
                                            <filter>QUANTITY_COMPARATOR</filter>
                                            <value>
                                                <xsl:value-of select="constrain_by_value/value_constraint"/>
                                            </value>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                    <!-- TODO: Display übernehmen -->
                                </valueFilter>
                            </xsl:if>
                        </criterion>
                    </xsl:for-each>
                </inclusionCriteria>
            </xsl:for-each>
        </query>
    </xsl:template>
</xsl:stylesheet>