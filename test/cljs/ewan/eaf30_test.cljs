(ns ewan.eaf30-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [ewan.eaf30 :as eaf]))

(deftest example-0
  (testing "a small EAF file that should parse"
    (is (= true
           (eaf/eaf?
            (eaf/eaf-str->hiccup
             "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<ANNOTATION_DOCUMENT AUTHOR=\"\" DATE=\"2018-03-24T18:09:00-06:00\" FORMAT=\"3.0\" VERSION=\"3.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://www.mpi.nl/tools/elan/EAFv3.0.xsd\">
    <HEADER MEDIA_FILE=\"\" TIME_UNITS=\"milliseconds\">
        <PROPERTY NAME=\"URN\">urn:nl-mpi-tools-elan-eaf:c45b7718-9185-49a2-8ff4-f59257751cb2</PROPERTY>
        <PROPERTY NAME=\"lastUsedAnnotationId\">2</PROPERTY>
    </HEADER>
    <TIME_ORDER>
        <TIME_SLOT TIME_SLOT_ID=\"ts4\" TIME_VALUE=\"3070\"/>
    </TIME_ORDER>
    <TIER LINGUISTIC_TYPE_REF=\"default-lt\" TIER_ID=\"default\"/>
    <TIER ANNOTATOR=\"quang phuc dong\" LINGUISTIC_TYPE_REF=\"default-lt\" TIER_ID=\"tier1\">
        <ANNOTATION>
            <ALIGNABLE_ANNOTATION ANNOTATION_ID=\"a1\" TIME_SLOT_REF1=\"ts1\" TIME_SLOT_REF2=\"ts2\">
                <ANNOTATION_VALUE>qwe</ANNOTATION_VALUE>
            </ALIGNABLE_ANNOTATION>
        </ANNOTATION>
    </TIER>
    <LINGUISTIC_TYPE CONSTRAINTS=\"Symbolic_Subdivision\" CONTROLLED_VOCABULARY_REF=\"vocab1\" GRAPHIC_REFERENCES=\"false\" LINGUISTIC_TYPE_ID=\"type2\" TIME_ALIGNABLE=\"false\"/>
    <LANGUAGE LANG_DEF=\"http://cdb.iso.org/lg/CDB-00130975-001\" LANG_ID=\"und\" LANG_LABEL=\"undetermined (und)\"/>
    <CONSTRAINT DESCRIPTION=\"Time alignable annotations within the parent annotation's time interval, gaps are allowed\" STEREOTYPE=\"Included_In\"/>
    <CONTROLLED_VOCABULARY CV_ID=\"vocab1\">
        <DESCRIPTION LANG_REF=\"und\">Hello world	</DESCRIPTION>
        <CV_ENTRY_ML CVE_ID=\"cveid2\">
            <CVE_VALUE DESCRIPTION=\"qlkwe\" LANG_REF=\"und\">waq</CVE_VALUE>
        </CV_ENTRY_ML>
    </CONTROLLED_VOCABULARY>
</ANNOTATION_DOCUMENT>"))))))

(deftest example-1
  (testing "an EAF file that should parse"
    (is (= true
           (eaf/eaf?
            (eaf/eaf-str->hiccup
             "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<ANNOTATION_DOCUMENT AUTHOR=\"\" DATE=\"2018-03-24T18:09:00-06:00\" FORMAT=\"3.0\" VERSION=\"3.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://www.mpi.nl/tools/elan/EAFv3.0.xsd\">
    <HEADER MEDIA_FILE=\"\" TIME_UNITS=\"milliseconds\">
        <MEDIA_DESCRIPTOR MEDIA_URL=\"file:///Users/lukegessler/Documents/avi/video-1516109432.mp4\" MIME_TYPE=\"video/mp4\" RELATIVE_MEDIA_URL=\"../Documents/avi/video-1516109432.mp4\"/>
        <PROPERTY NAME=\"lastUsedAnnotationId\">2</PROPERTY>
    </HEADER>
    <TIME_ORDER>
        <TIME_SLOT TIME_SLOT_ID=\"ts4\" TIME_VALUE=\"3070\"/>
    </TIME_ORDER>
    <TIER LINGUISTIC_TYPE_REF=\"default-lt\" TIER_ID=\"default\"/>
    <TIER ANNOTATOR=\"quang phuc dong\" LINGUISTIC_TYPE_REF=\"default-lt\" TIER_ID=\"tier1\">
        <ANNOTATION>
            <ALIGNABLE_ANNOTATION ANNOTATION_ID=\"a2\" TIME_SLOT_REF1=\"ts3\" TIME_SLOT_REF2=\"ts4\">
                <ANNOTATION_VALUE>qwoi</ANNOTATION_VALUE>
            </ALIGNABLE_ANNOTATION>
        </ANNOTATION>
    </TIER>
    <TIER LINGUISTIC_TYPE_REF=\"default-lt\" TIER_ID=\"tier3\"/>
    <LINGUISTIC_TYPE CONSTRAINTS=\"Symbolic_Subdivision\" CONTROLLED_VOCABULARY_REF=\"vocab1\" GRAPHIC_REFERENCES=\"false\" LINGUISTIC_TYPE_ID=\"type2\" TIME_ALIGNABLE=\"false\"/>
    <LANGUAGE LANG_DEF=\"http://cdb.iso.org/lg/CDB-00130975-001\" LANG_ID=\"und\" LANG_LABEL=\"undetermined (und)\"/>
    <CONSTRAINT DESCRIPTION=\"Time alignable annotations within the parent annotation's time interval, gaps are allowed\" STEREOTYPE=\"Included_In\"/>
    <CONTROLLED_VOCABULARY CV_ID=\"vocab1\">
        <DESCRIPTION LANG_REF=\"und\">Hello world	</DESCRIPTION>
        <CV_ENTRY_ML CVE_ID=\"cveid4\">
            <CVE_VALUE DESCRIPTION=\"qlkjwe\" LANG_REF=\"und\">qlkewjqlwkej</CVE_VALUE>
        </CV_ENTRY_ML>
    </CONTROLLED_VOCABULARY>
</ANNOTATION_DOCUMENT>"))))))

(def ^:private mini-eaf (eaf/create-eaf {:author "Luke"
                                         :date "2018-03-24T18:09:00-06:00"
                                         :media-descriptors
                                         [{:mime-type "video/mp4" :media-url "a.mp4"}
                                          {:mime-type "audio/wav" :media-url "a.wav"}]}))

(deftest get-0
  (testing "get-date returns annotation-document's date"
    (is (= "2018-03-24T18:09:00-06:00"
           (eaf/get-date mini-eaf)))))

