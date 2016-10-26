package org.geotools.image.test;

import java.awt.image.RenderedImage;

public class ImageDialog extends ReferenceImageDialog {
    
    public ImageDialog(RenderedImage image) {
        super(ImageAssert.realignImage(image));
    }
    
    public static boolean show(RenderedImage ri) {
        return ReferenceImageDialog.show(ImageAssert.realignImage(ri));
    }
}
