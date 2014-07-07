package com.trifork.android.tools.svgconverter

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.api.DefaultAndroidSourceDirectorySet
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.JPEGTranscoder
import org.apache.batik.transcoder.image.PNGTranscoder
import org.apache.commons.io.FilenameUtils
import org.apache.tools.ant.types.resources.Files
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.StopExecutionException

// see http://www.gradle.org/docs/current/userguide/custom_plugins.html

public class PluginTemplate implements Plugin<Project> {


    Object android
    def rootDir

    @Override
    void apply(Project project) {
        if (!hasAndroidPlugin(project)) {
            throw new StopExecutionException(
                    "Must be applied before 'android' or 'android-library' plugin.")

        }
        rootDir = project.buildDir;
        android = project.extensions.getByName("android");
        def svgInputs = project.container(SVGInput);

        svgInputs.add(new SVGInput("launcher",48,48));
        svgInputs.add(new SVGInput("actionbar",32,32));
        svgInputs.add(new SVGInput("notification",24,24));
        svgInputs.add(new SVGInput("contextual",16,16));

        project.extensions.SVGInputs = svgInputs;


        def pngOutputs = project.container(PNGOutput);
        project.extensions.PNGOutputs = pngOutputs;


        project.afterEvaluate {
            android.applicationVariants.each { variant ->

                def task = project.task("svgConverter" + variant.name) << {

                    def variantObj = (ApplicationVariant) variant

                    def sets = variantObj.getSourceSets();
                    DefaultAndroidSourceDirectorySet androidSourceDirectorySet = new DefaultAndroidSourceDirectorySet("svg", project);
                    def directories = []
                    sets.iterator().each { sourceDir ->

                        def var = "src/" + sourceDir.name
                        directories.add(var)


                    }

                    androidSourceDirectorySet.setSrcDirs directories

                    androidSourceDirectorySet.srcDirs.each { srcDir ->
                        println "SrcDir " + srcDir.path

                        svgInputs.each { svgInput ->
                            def svgInputPath = new File(srcDir.getPath(),"svg/" + svgInput.name)
                            println "SVG Input " + svgInputPath

                            println "PMG output " + svgInputPath
                            svgInputPath.listFiles().each { svgFile ->
                                println "SVG file " + svgFile.getPath()

                                println "SVG width " + svgInput.width
                                println "SVG height " + svgInput.height
                                pngOutputs.each { pngOutput ->

                                    def outPutFileName = "${srcDir.getPath()}/res/drawable-${pngOutput.name}/${FilenameUtils.removeExtension(svgFile.getName())}.png"
                                    def w = (int) (svgInput.width * pngOutput.factor);
                                    def h = (int) (svgInput.height * pngOutput.factor);

/**
                                    // create a JPEG transcoder
                                    PNGTranscoder t = new PNGTranscoder();
                                    // set the transcoding hints
                                    t.addTranscodingHint(PNGTranscoder.KEY_HEIGHT,new Float(h));
                                    t.addTranscodingHint(PNGTranscoder.KEY_WIDTH,new Float(w));
                                    t.addTranscodingHint(PNGTranscoder.KEY_MAX_HEIGHT,new Float(h));
                                    t.addTranscodingHint(PNGTranscoder.KEY_MAX_WIDTH,new Float(w));
                                    t.addTranscodingHint(PNGTranscoder.KEY_PIXEL_UNIT_TO_MILLIMETER, new Float(0.3528f));

                                    // create the transcoder input
                                    TranscoderInput input = new TranscoderInput(new FileInputStream(svgFile));
                                    // create the transcoder output
                                    OutputStream ostream = new FileOutputStream(outPutFileName);
                                    TranscoderOutput output = new TranscoderOutput(ostream);
                                    // save the image
                                    t.transcode(input, output);
                                    // flush and close the stream then exit
                                    ostream.flush();
                                    ostream.close();
 **/
                                    def proc = "rsvg-convert -d 72 -p 72 -w ${w} -h ${h} -f png  ${svgFile.getPath()} -o ${outPutFileName} ".execute()
                                    proc.waitFor();
                                    println "return code: ${proc.exitValue()}"
                                    println "stderr: ${proc.err.text}"
                                    println "stdout: ${proc.in.text}"

                                }


                            }

                        }

                    }

                }
                ((ApplicationVariant) variant).getMergeAssets().dependsOn(task);

            }


        }


    }

    static def hasAndroidPlugin(Project project) {
        return project.plugins.hasPlugin(AppPlugin) || project.plugins.hasPlugin(LibraryPlugin)
    }

    /**
     * Returns directory for plugin's private working directory for argument
     *
     * @param variant the Variant
     * @return
     */
    File getVariantWorkDir(Object variant) {
        new File([rootDir, "variant", variant.name].join(File.separator))
    }

}

class SVGInput {
    float width
    float height
    private final String name
    SVGInput(String name) {
        this.name = name
    }

    SVGInput(String name, float width,float height) {
        this.name = name;
        this.width = width;
        this.height = height;

    }
}


class PNGOutput {
    float    factor
    private final String name

    PNGOutput(String name) {
        this.name = name
    }
}
